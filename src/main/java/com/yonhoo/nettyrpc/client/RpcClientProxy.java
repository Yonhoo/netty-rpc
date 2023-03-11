package com.yonhoo.nettyrpc.client;

import java.lang.reflect.InvocationHandler;

public class RpcClientProxy {
    private final InvokerBroker invokerBroker;

    public RpcClientProxy(InvokerBroker invokerBroker) {
        this.invokerBroker = invokerBroker;
    }

    public <T> T getProxy() throws ClassNotFoundException {
        Class<T> interfaceClass = (Class<T>) Class.forName(invokerBroker.getConsumerConfig().getConsumerInterface());
        InvocationHandler handler = new ProxyInvoker(interfaceClass, invokerBroker);
        ClassLoader classLoader = this.getClass().getClassLoader();
        return (T) java.lang.reflect.Proxy.newProxyInstance(classLoader,
                new Class[] {interfaceClass}, handler);
    }
}
