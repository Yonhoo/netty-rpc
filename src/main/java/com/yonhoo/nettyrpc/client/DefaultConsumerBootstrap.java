package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
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

    private static DefaultClientConnectionManager defaultClientConnectionManager = new DefaultClientConnectionManager();
    private T proxyInvoker;

    private Registry registry;
    private static final ConcurrentMap<ConsumerConfig, InvokerBroker> consumerInvokers = new ConcurrentHashMap<>();

    public DefaultConsumerBootstrap(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

//    static {
//        Runtime.getRuntime().addShutdownHook(new Thread(DefaultConsumerBootstrap::close));
//    }

    public T refer() throws Exception {
        if (proxyInvoker != null) {
            return proxyInvoker;
        }

        synchronized (this) {
            if (proxyInvoker != null) {
                return proxyInvoker;
            }

            invokerBroker = new InvokerBroker(consumerConfig, defaultClientConnectionManager);

            if (consumerConfig.getProviderInfoListener() == null) {
                consumerConfig.setProviderInfoListener(new DefaultProviderInfoListener(invokerBroker));
            }

            RegistryPropertiesConfig propertiesConfig = new RegistryPropertiesConfig();
            propertiesConfig.setAddress("127.0.0.1");
            propertiesConfig.setApplication("test-application");
            propertiesConfig.setPort(2181);
            registry = new ZookeeperRegistry(propertiesConfig);

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

    public void close() {
        // 1. close consumer invokers
        consumerInvokers.values().forEach(InvokerBroker::destroy);

        // 2. close connection manager & netty client
        defaultClientConnectionManager.close();

        // 3. close registry
        registry.destroy();

        //TODO need enhance
        // 4. remove provider infos
        consumerInvokers.forEach((key, value) -> value.clearProviderInfos());
    }
}
