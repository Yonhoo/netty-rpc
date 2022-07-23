package com.yonhoo.nettyrpc.server;


import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;

public class ServerServiceDefinition {
    private final String serviceName;
    private final Object serviceImpl;
    private final Class<?> classType;

    public ServerServiceDefinition(String serviceName, Object serviceImpl, Class<?> classType) {
        this.serviceName = serviceName;
        this.serviceImpl = serviceImpl;
        this.classType = classType;
    }

    public Object invokeMethod(String methodName, Class<?>[] parameters) {
        try {
            return serviceImpl.getClass().getMethod(methodName, parameters);
        } catch (Exception e) {
            throw RpcException.with(RpcErrorCode.SERVICE_NOT_THIS_METHOD);
        }
    }

    public Class<?> getClassType() {
        return classType;
    }

    public String getServiceName() {
        return serviceName;
    }
}
