package com.yonhoo.nettyrpc.registry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

@Slf4j
public class ZookeeperRegistry extends Registry {

    public static final String CONTEXT_SEP = "/";
    private CuratorFramework zkClient;

    public ZookeeperRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    public void init() {
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

    }

    @Override
    public boolean start() {
        if (zkClient == null) {
            log.warn("Start zookeeper registry must be do init first!");
            return false;
        }
        if (zkClient.getState() == CuratorFrameworkState.STARTED) {
            return true;
        }

        zkClient.start();

        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void registry(ProviderConfig providerConfig) {
        if (providerConfig.isRegistered()) {
            log.warn("provider config already registered: {}", providerConfig);
            return;
        }

        try {

            String providerPath = ZookeeperRegistryHelper.buildProviderPath(registryConfig.getRootPath(),
                    providerConfig);

            for (ServiceConfig serviceConfig : providerConfig.getServiceConfigList()) {
                String metaData = ZookeeperRegistryHelper.convertMetaData(providerConfig, serviceConfig);

                String encodeData = URLEncoder.encode(metaData, StandardCharsets.UTF_8);

                String servicePath = providerPath + CONTEXT_SEP + serviceConfig.getUrl();

                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath, encodeData.getBytes());

            }
        } catch (Exception e) {
            log.error("registry config error", e);
        }
    }

    private CuratorFramework getAndCheckZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new RuntimeException("registry client not available");
        }
        return zkClient;
    }

    @Override
    public boolean unRegistry(ProviderConfig providerConfig) {
        return false;
    }

    @Override
    public List<ProviderInfo> subscribe(ConsumerConfig config) {
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {

    }

    @Override
    public void destroy() {

    }
}
