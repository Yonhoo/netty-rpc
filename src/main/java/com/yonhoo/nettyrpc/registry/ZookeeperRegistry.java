package com.yonhoo.nettyrpc.registry;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_UPDATED;

import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZookeeperRegistry implements Registry {

    public static final String CONTEXT_SEP = "/";
    private CuratorFramework zkClient;
    private final RegistryConfig registryConfig;
    private final ZookeeperProviderObserver providerObserver = new ZookeeperProviderObserver();
    private static final ConcurrentMap<ConsumerConfig, CuratorCache>
            INTERFACE_PROVIDER_CACHE = new ConcurrentHashMap<>();

    public ZookeeperRegistry(RegistryPropertiesConfig registryPropertiesConfig) {
        registryConfig = RegistryConfig.builder()
                // unique application
                .rootPath(registryPropertiesConfig.getApplication())
                .address(registryPropertiesConfig.getAddress())
                .connectTimeout(60000)
                .port(registryPropertiesConfig.getPort())
                .build();

        if (!registryConfig.getRootPath().endsWith(CONTEXT_SEP)) {
            registryConfig.setRootPath(registryConfig.getRootPath() + CONTEXT_SEP);
        }

        String address = registryConfig.getAddress() + ":" + registryConfig.getPort();

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFrameworkFactory.Builder zkClientBuilder = CuratorFrameworkFactory.builder()
                .connectString(address)
                .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
                .connectionTimeoutMs(registryConfig.getConnectTimeout())
                .canBeReadOnly(false)
                .retryPolicy(retryPolicy)
                .defaultData(null);

        zkClient = zkClientBuilder.build();

        zkClient.start();

        if (CuratorFrameworkState.STARTED != zkClient.getState()) {
            throw RpcException.with(RpcErrorCode.REGISTRY_CLIENT_START_EXCEPTION);
        }
    }

    public CuratorFramework getZkClient() {
        return zkClient;
    }

    @Override
    public void registry(ProviderConfig providerConfig) {
        if (providerConfig.isRegistered()) {
            log.warn("provider config already registered: {}", providerConfig);
            return;
        }

        try {
            String providerPath = ZookeeperRegistryHelper.buildProviderPath(registryConfig.getRootPath(),
                    providerConfig.getProviderName());

            for (ServiceConfig serviceConfig : providerConfig.getServiceConfigList()) {
                String metaData = ZookeeperRegistryHelper.convertMetaData(providerConfig, serviceConfig);

                String encodeData = URLEncoder.encode(metaData, StandardCharsets.UTF_8);

                String servicePath = providerPath + CONTEXT_SEP + serviceConfig.getUrl();

                log.info("registry server path: {}", servicePath);

                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(servicePath, encodeData.getBytes());
            }
        } catch (Exception e) {
            log.error("registry config error", e);
        }

        providerConfig.setRegistered(true);
    }

    private CuratorFramework getAndCheckZkClient() {
        checkZkClient();
        return zkClient;
    }

    private void checkZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw RpcException.with(RpcErrorCode.REGISTRY_CLIENT_UNAVAILABLE);
        }
    }

    @Override
    public boolean unRegistry(ProviderConfig providerConfig) {
        if (!providerConfig.isRegistered()) {
            log.warn("provider config was not registered");
            return false;
        }
        try {

            String providerPath = ZookeeperRegistryHelper.buildProviderPath(registryConfig.getRootPath(),
                    providerConfig.getProviderName());

            for (ServiceConfig serviceConfig : providerConfig.getServiceConfigList()) {

                String servicePath = providerPath + CONTEXT_SEP + serviceConfig.getUrl();

                getAndCheckZkClient()
                        .delete()
                        .forPath(servicePath);

            }
        } catch (Exception e) {
            log.error("registry config error", e);
        }

        providerConfig.setRegistered(false);

        return true;
    }

    // no retry after subscribe fail
    @Override
    public void subscribe(final ConsumerConfig config) throws InterruptedException {

        checkZkClient();

        String providerPath = ZookeeperRegistryHelper.buildProviderPath(registryConfig.getRootPath(),
                config.getConsumerInterface());

        log.info("subscribe providerPath {}", providerPath);

        CuratorCache curatorCache = CuratorCache.build(zkClient, providerPath);

        CuratorCacheListener curatorCacheListener = CuratorCacheListener.builder()
                .forCreates(childData -> providerObserver.addProvider(config, providerPath, childData))
                .forChanges((oldChildData, childData) -> providerObserver.updateProvider(config, providerPath, childData))
                .forDeletes(childData -> providerObserver.removeProvider(config, providerPath, childData))
                .build();

        curatorCache.listenable().addListener(curatorCacheListener);

        curatorCache.start();

        INTERFACE_PROVIDER_CACHE.put(config, curatorCache);
        providerObserver.addProviderListener(config, config.getProviderInfoListener());

    }

    @Override
    public void subscribe(ConsumerConfig config, long waitTime, TimeUnit timeUnit) throws InterruptedException {
        this.subscribe(config);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(waitTime, timeUnit);
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        checkZkClient();

        providerObserver.removeProviderListener(config);
        CuratorCache childCache = INTERFACE_PROVIDER_CACHE.remove(config);
        if (childCache != null) {
            try {
                childCache.close();
            } catch (Exception e) {
                log.error("unsubscribe consumer config : {}", e);
                throw RpcException.with(RpcErrorCode.REGISTRY_UNSUBSCRIBE_EXCEPTION);
            }
        }

    }

    @Override
    public void destroy() {
        INTERFACE_PROVIDER_CACHE.forEach((consumerConfig, curatorCache) -> curatorCache.close());

        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            zkClient.close();
        }
    }
}
