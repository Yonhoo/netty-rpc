package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZookeeperRegistry implements Registry {

    public static final String CONTEXT_SEP = "/";
    private CuratorFramework zkClient;
    private RegistryConfig registryConfig;
    private ZookeeperProviderObserver providerObserver = new ZookeeperProviderObserver();
    private RegistryPropertiesConfig registryPropertiesConfig;
    private static final ConcurrentMap<ConsumerConfig, CuratorCache> INTERFACE_PROVIDER_CACHE
            = new ConcurrentHashMap<ConsumerConfig, CuratorCache>();

    public ZookeeperRegistry(RegistryPropertiesConfig registryPropertiesConfig) {
        registryConfig = RegistryConfig.builder()
                .address(registryPropertiesConfig.getAddress())
                .port(registryPropertiesConfig.getPort())
                .build();

        if (zkClient != null) {
            return;
        }

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

                String servicePath = providerPath + "=" + serviceConfig.getUrl();

                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath, encodeData.getBytes());

            }
        } catch (Exception e) {
            log.error("registry config error", e);
        }

        providerConfig.setRegistered(true);
    }

    private CuratorFramework getAndCheckZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw RpcException.with(RpcErrorCode.REGISTRY_CLIENT_UNAVAILABLE);
        }
        return zkClient;
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

        //TODO remove configObserver cache

        return true;
    }

    // no retry after subscribe fail
    @Override
    public List<ProviderInfo> subscribe(final ConsumerConfig config) {

        String providerPath = ZookeeperRegistryHelper.buildProviderPath(registryConfig.getRootPath(),
                config.getConsumerInterface());

        log.info("subscribe providerPath {}", providerPath);

        CuratorCache curatorCache = CuratorCache.build(zkClient, providerPath);

        // provider root listener
        curatorCache.listenable().addListener(((type, beforeData, updateData) -> {
            switch (type) {
                case NODE_CHANGED:
                    providerObserver.updateProvider(config, providerPath, beforeData,
                            List.of(updateData));
                    break;
                case NODE_CREATED:
                    providerObserver.addProvider(config, providerPath, beforeData,
                            List.of(updateData));
                    break;
                case NODE_DELETED:
                    providerObserver.removeProvider(config, providerPath, beforeData,
                            List.of(updateData));
                    break;
                default:
                    break;
            }
        }));

        curatorCache.start();
        INTERFACE_PROVIDER_CACHE.put(config, curatorCache);
        return curatorCache.stream()
                .map(this::getProviderInfo)
                .collect(Collectors.toList());
    }

    private ProviderInfo getProviderInfo(ChildData childData) {
        Map<String, String> providerInfoMap =
                ZookeeperRegistryHelper.convertMetaDataToMap(Arrays.toString(childData.getData()));

        log.info("childData path {}", childData.getPath());
        return ProviderInfo.builder()
                .address(childData.getPath())
                .port(80)
                .providerName(providerInfoMap.get(RpcConstants.PROVIDER_NAME))
                .weight(Integer.parseInt(providerInfoMap.get(RpcConstants.SERVICE_WEIGHT)))
                .build();
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
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
