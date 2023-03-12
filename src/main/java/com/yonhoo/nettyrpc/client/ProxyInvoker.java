package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
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
    public Object invoke(Object proxy, Method method, Object[] args) {
        ConsumerConfig consumerConfig = invokerBroker.getConsumerConfig();
        String methodName = method.getName();
        Class[] paramTypes = method.getParameterTypes();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(consumerConfig.getConsumerInterface())
                .parameters(args)
                .paramTypes(paramTypes)
                .methodName(methodName)
                .version("1.0")
                .build();

        return invokerBroker.doInvoke(rpcRequest);
    }
}
