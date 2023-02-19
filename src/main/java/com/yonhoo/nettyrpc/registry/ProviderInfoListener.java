package com.yonhoo.nettyrpc.registry;

import java.util.List;

public interface ProviderInfoListener {
    void addProviders(List<ProviderInfo> providerList);

    void removeProviders(List<ProviderInfo> providerList);

    void updateProviders(List<ProviderInfo> providerList);
}
