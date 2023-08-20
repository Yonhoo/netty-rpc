package com.yonhoo.nettyrpc.server;


import com.yonhoo.nettyrpc.common.Destroyable;
import com.yonhoo.nettyrpc.common.ExecutorUtil;
import com.yonhoo.nettyrpc.exception.RpcException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerServiceDefinition implements Destroyable {
    private final String serviceName;
    private final Object serviceImpl;
    private final Class<?> classType;
    private final int weight;
    private boolean destroyed = false;

    private final int closeTimeout;
    private final ExecutorService bizThreadPool;

    public ServerServiceDefinition(String serviceName, Object serviceImpl,
                                   Class<?> classType, int weight,
                                   ExecutorService bizThreadPool,
                                   int closeTimeout) {
        this.serviceName = serviceName;
        this.serviceImpl = serviceImpl;
        this.classType = classType;
        this.weight = weight;
        this.bizThreadPool = bizThreadPool;
        this.closeTimeout = closeTimeout;
    }

    public Object invokeMethod(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        try {
            log.info(serviceName + " invoke " + methodName);
            if (bizThreadPool != null) {
                Future<Object> futureResult = bizThreadPool.submit(
                        () -> invokeMethodByName(methodName, parameterTypes, parameters));
                return futureResult.get();
            }

            return invokeMethodByName(methodName, parameterTypes, parameters);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException
                 | InvocationTargetException | IllegalAccessException
                 | ExecutionException | InterruptedException e) {
            String errorMsg = e.getMessage();
            if (e instanceof InvocationTargetException) {
                errorMsg = ((InvocationTargetException) e).getTargetException().getMessage();
            }
            throw new RpcException(errorMsg, e);
        }
    }

    private Object invokeMethodByName(String methodName, Class<?>[] parameterTypes, Object[] parameters) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = serviceImpl.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(serviceImpl, parameters);
    }

    public int getWeight() {
        return weight;
    }

    public Class<?> getClassType() {
        return classType;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (bizThreadPool != null) {
            log.info("service {} bizThreadPool now graceful shutdown", serviceName);
            ExecutorUtil.gracefulShutdown(bizThreadPool, closeTimeout);
        }
    }
}
