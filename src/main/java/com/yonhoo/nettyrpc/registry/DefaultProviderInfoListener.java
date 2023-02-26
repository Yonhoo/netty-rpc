package com.yonhoo.nettyrpc.registry;


public class DefaultProviderInfoListener implements ProviderInfoListener {
    @Override
    public void addProvider(ProviderInfo providerInfo) {
        System.out.println(providerInfo);
    }

    @Override
    public void removeProvider(ProviderInfo providerInfo) {
        System.out.println(providerInfo);
    }

    @Override
    public void updateProvider(ProviderInfo providerInfo) {
        System.out.println(providerInfo);
    }
}
