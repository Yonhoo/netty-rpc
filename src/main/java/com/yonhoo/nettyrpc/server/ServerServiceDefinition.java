package com.yonhoo.nettyrpc.server;


import com.yonhoo.nettyrpc.exception.RpcException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerServiceDefinition {
    private final String serviceName;
    private final Object serviceImpl;
    private final Class<?> classType;

    public ServerServiceDefinition(String serviceName, Object serviceImpl, Class<?> classType) {
        this.serviceName = serviceName;
        this.serviceImpl = serviceImpl;
        this.classType = classType;
    }

    public Object invokeMethod(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        try {
            log.info(serviceName + " invoke " + methodName);
            Method method = serviceImpl.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(serviceImpl, parameters);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                 | IllegalAccessException e) {
            String errorMsg = e.getMessage();
            if (e instanceof InvocationTargetException) {
                errorMsg = ((InvocationTargetException) e).getTargetException().getMessage();
            }
            throw new RpcException(errorMsg, e);
        }
    }

    public Class<?> getClassType() {
        return classType;
    }

    public String getServiceName() {
        return serviceName;
    }
}
