package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.Destroyable;
import com.yonhoo.nettyrpc.load_balancer.DefaultLoadBalancer;
import com.yonhoo.nettyrpc.load_balancer.LoadBalancer;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.registry.ProviderInfo;
import com.yonhoo.nettyrpc.registry.ProviderInfoListener;

import java.util.concurrent.CopyOnWriteArrayList;

public class InvokerBroker implements ProviderInfoListener, Destroyable {
    private ConsumerConfig consumerConfig;
    private final LoadBalancer loadBalancer = new DefaultLoadBalancer();
    private final CopyOnWriteArrayList<ProviderInfo> providerInfos = new CopyOnWriteArrayList<>();
    private final DefaultClientTransport defaultClientTransport = new DefaultClientTransport();

    public InvokerBroker(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Override
    public void addProvider(ProviderInfo providerInfo) {
        //TODO create connection
        providerInfos.add(providerInfo);
    }

    @Override
    public void removeProvider(ProviderInfo providerInfo) {
        //TODO remove connection
        providerInfos.remove(providerInfo);
    }


    @Override
    public void updateProvider(ProviderInfo providerInfo) {
        //update connection
        //update provider info
    }

    public Object doInvoke(RpcRequest request) {
        if (providerInfos.isEmpty()) {
            // FixMe
        }
        InvokeContext invokeContext = new InvokeContext();

        //load balancer
        ProviderInfo providerInfo = loadBalancer.select(providerInfos);
        invokeContext.setProviderInfo(providerInfo);

        return defaultClientTransport.doSend(request, invokeContext);
    }

    public void clearProviderInfos() {
        providerInfos.clear();
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    @Override
    public void destroy() {
        defaultClientTransport.destroy();
    }
}
