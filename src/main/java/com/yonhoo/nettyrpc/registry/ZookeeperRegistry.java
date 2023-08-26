package com.yonhoo.nettyrpc.registry;


import com.yonhoo.nettyrpc.common.Destroyable;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.yonhoo.nettyrpc.util.RegistryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZookeeperRegistry implements Registry, Destroyable {

    public static final String CONTEXT_SEP = "/";
    private CuratorFramework zkClient;
    private final RegistryConfig registryConfig;
    private final ZookeeperProviderObserver providerObserver = new ZookeeperProviderObserver();
    private static final ConcurrentMap<ConsumerConfig, CuratorCache>
            INTERFACE_PROVIDER_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentLinkedQueue<String> SERVICE_PATHS = new ConcurrentLinkedQueue<>();

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
            log.error("client status: {}", zkClient.getState().toString());
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

                SERVICE_PATHS.add(servicePath);

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
            log.error("client status: {}", zkClient.getState().toString());
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
        log.info("unSubscribe consumer {}", config.getConsumerInterface());
        providerObserver.removeProviderListener(config);
        CuratorCache childCache = INTERFACE_PROVIDER_CACHE.remove(config);
        if (childCache != null) {
            try {
                List<ProviderInfo> providerInfos = childCache.stream()
                        .filter(childData -> !RegistryUtils.isRootPath(childData.getPath()))
                        .map(childData -> {
                            String providerPath = RegistryUtils.getProviderPath(childData.getPath());
                            String rootPath = RegistryUtils.getRootPath(childData.getPath());
                            String[] portWithAddress = providerPath.split(":");
                            Map<String, String> dataMap = RegistryUtils.buildStringMapFromBytes(childData);

                            return ProviderInfo.builder()
                                    .rootPath(rootPath)
                                    .servicePath(childData.getPath())
                                    .port(Integer.valueOf(portWithAddress[1]))
                                    .address(portWithAddress[0])
                                    .weight(Double.parseDouble(dataMap.getOrDefault(RpcConstants.SERVICE_WEIGHT, "0.0")))
                                    .providerName(dataMap.getOrDefault(RpcConstants.PROVIDER_NAME, null))
                                    .build();
                        })
                        .collect(Collectors.toList());

                providerInfos.stream().forEach(providerInfo -> {
                    log.info("remove provider {}", providerInfo);
                    config.getProviderInfoListener().removeProvider(providerInfo);
                });

                childCache.close();
            } catch (Exception e) {
                log.error("unsubscribe consumer config : {}", e);
                throw RpcException.with(RpcErrorCode.REGISTRY_UNSUBSCRIBE_EXCEPTION);
            }
        }

    }

    @Override
    public void destroy() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            INTERFACE_PROVIDER_CACHE.forEach((consumerConfig, curatorCache) ->
                    unSubscribe(consumerConfig));

            INTERFACE_PROVIDER_CACHE.clear();

            SERVICE_PATHS.forEach((servicePath) -> {
                try {
                    getAndCheckZkClient()
                            .delete()
                            .forPath(servicePath);
                } catch (Exception e) {
                    log.warn("delete service path error {}", servicePath, e);
                }
            });

            SERVICE_PATHS.clear();

            zkClient.close();
        }
    }
}
