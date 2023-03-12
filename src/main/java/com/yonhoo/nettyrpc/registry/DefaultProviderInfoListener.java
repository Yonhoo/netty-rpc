package com.yonhoo.nettyrpc.registry;


import com.yonhoo.nettyrpc.client.InvokerBroker;

public class DefaultProviderInfoListener implements ProviderInfoListener {

    private final InvokerBroker invokerBroker;

    public DefaultProviderInfoListener(InvokerBroker invokerBroker) {
        this.invokerBroker = invokerBroker;
    }

    @Override
    public void addProvider(ProviderInfo providerInfo) {
        invokerBroker.addProvider(providerInfo);
    }

    @Override
    public void removeProvider(ProviderInfo providerInfo) {
        invokerBroker.removeProvider(providerInfo);
    }

    @Override
    public void updateProvider(ProviderInfo providerInfo) {
        invokerBroker.updateProvider(providerInfo);
    }
}
