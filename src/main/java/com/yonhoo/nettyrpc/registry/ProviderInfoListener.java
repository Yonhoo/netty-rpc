package com.yonhoo.nettyrpc.registry;

public interface ProviderInfoListener {
    void addProvider(ProviderInfo providerInfo);

    void removeProvider(ProviderInfo providerInfo);

    void updateProvider(ProviderInfo providerInfo);
}
