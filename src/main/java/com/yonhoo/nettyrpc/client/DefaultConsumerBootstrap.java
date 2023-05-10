package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.registry.DefaultProviderInfoListener;
import com.yonhoo.nettyrpc.registry.Registry;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DefaultConsumerBootstrap<T> {
    private final ConsumerConfig consumerConfig;
    private InvokerBroker invokerBroker;
    private T proxyInvoker;

    public DefaultConsumerBootstrap(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public T refer() throws Exception {
        if (proxyInvoker != null) {
            return proxyInvoker;
        }

        synchronized (this) {
            if (proxyInvoker != null) {
                return proxyInvoker;
            }

            invokerBroker = new InvokerBroker(consumerConfig);

            if (consumerConfig.getProviderInfoListener() == null) {
                consumerConfig.setProviderInfoListener(new DefaultProviderInfoListener(invokerBroker));
            }

            Registry registry = ApplicationContextUtil.getBean(ZookeeperRegistry.class);

            if (Objects.isNull(registry)) {
                throw RpcException.with(RpcErrorCode.REGISTRY_CLIENT_UNAVAILABLE);
            }

            // refer fast , so might get empty provider info before get notify from registry
            registry.subscribe(consumerConfig, 2, TimeUnit.SECONDS);

            RpcClientProxy rpcClientProxy = new RpcClientProxy(invokerBroker);

            proxyInvoker = rpcClientProxy.getProxy();

            return proxyInvoker;
        }
    }
}
