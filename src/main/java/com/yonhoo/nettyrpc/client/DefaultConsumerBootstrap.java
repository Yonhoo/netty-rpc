package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import com.yonhoo.nettyrpc.connection.ClientConnectionManager;
import com.yonhoo.nettyrpc.connection.DefaultClientConnectionManager;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.registry.DefaultProviderInfoListener;
import com.yonhoo.nettyrpc.registry.Registry;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultConsumerBootstrap<T> {
    private final ConsumerConfig consumerConfig;
    private InvokerBroker invokerBroker;
    private T proxyInvoker;

    private static final ConcurrentMap<ConsumerConfig, InvokerBroker> consumerInvokers = new ConcurrentHashMap<>();

    public DefaultConsumerBootstrap(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DefaultConsumerBootstrap::close));
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

            consumerInvokers.put(consumerConfig, invokerBroker);

            return proxyInvoker;
        }

    }

    public static void close() {
        // 1. close consumer invokers
        consumerInvokers.values().forEach(InvokerBroker::destroy);

        // 2. close connection manager & netty client
        DefaultClientConnectionManager clientConnectionManager = ApplicationContextUtil
                .getBean(DefaultClientConnectionManager.class);

        Optional.ofNullable(clientConnectionManager).ifPresent(ClientConnectionManager::close);

        // 3. close registry
        Registry registry = ApplicationContextUtil.getBean(ZookeeperRegistry.class);
        Optional.ofNullable(registry).ifPresent(Registry::destroy);

        //TODO need enhance
        // 4. remove provider infos
        consumerInvokers.forEach((key, value) -> value.clearProviderInfos());
    }
}
