package com.yonhoo.nettyrpc.registry;

import java.util.List;

public abstract class Registry {
    protected RegistryConfig registryConfig;

    protected Registry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    public abstract boolean start();

    public abstract boolean registry(ProviderConfig providerConfig);

    public abstract boolean unRegistry(ProviderConfig providerConfig);

    public abstract List<ProviderInfo> subscribe(ConsumerConfig config);

    public abstract void unSubscribe(ConsumerConfig config);

    public abstract void destroy();
}
