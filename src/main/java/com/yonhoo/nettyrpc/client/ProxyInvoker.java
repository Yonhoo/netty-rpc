package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.protocol.RpcRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyInvoker implements InvocationHandler {

    private final Class<?> interfaceClass;
    private final InvokerBroker invokerBroker;

    public ProxyInvoker(Class<?> interfaceClass, InvokerBroker invokerBroker) {
        this.interfaceClass = interfaceClass;
        this.invokerBroker = invokerBroker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder().build();
        return invokerBroker.doInvoke(rpcRequest);
    }
}
