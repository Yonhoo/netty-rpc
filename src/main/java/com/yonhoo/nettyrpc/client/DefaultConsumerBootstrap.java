package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.registry.DefaultProviderInfoListener;
import com.yonhoo.nettyrpc.registry.Registry;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistry;

public class DefaultConsumerBootstrap<T> {
    private final ConsumerConfig consumerConfig;
    private InvokerBroker invokerBroker;
    private T proxyInvoker;

    public DefaultConsumerBootstrap(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public T refer() throws Exception {
        if (proxyInvoker == null) {
            return proxyInvoker;
        }

        synchronized (this) {
            if (proxyInvoker == null) {
                return proxyInvoker;
            }

            invokerBroker = new InvokerBroker(consumerConfig);

            if (consumerConfig.getProviderInfoListener() == null) {
                consumerConfig.setProviderInfoListener(new DefaultProviderInfoListener(invokerBroker));
            }

            Registry registry = (Registry) ApplicationContextUtil.getBean(ZookeeperRegistry.class.getName());

            registry.subscribe(consumerConfig);

            RpcClientProxy rpcClientProxy = new RpcClientProxy(invokerBroker);

            proxyInvoker = rpcClientProxy.getProxy();

            return proxyInvoker;
        }
    }
}
