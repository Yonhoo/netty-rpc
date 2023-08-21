package com.yonhoo.nettyrpc.server;


import com.yonhoo.nettyrpc.common.Destroyable;
import com.yonhoo.nettyrpc.common.ExecutorUtil;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerServiceDefinition implements Destroyable {
    private final String serviceName;
    private final Object serviceImpl;
    private final Class<?> classType;
    private final int weight;
    private final int closeTimeout;
    private final ExecutorService bizThreadPool;

    private final AtomicBoolean available = new AtomicBoolean(true);

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
        if (!available.get()) {
            throw RpcException.with(RpcErrorCode.SERVICE_NOT_REGISTERED);
        }

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

    public boolean isAvailable() {
        return available.get();
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
        if (!available.compareAndSet(true, false)) {
            return;
        }
        if (bizThreadPool != null) {
            log.info("service {} bizThreadPool now graceful shutdown", serviceName);
            ExecutorUtil.gracefulShutdown(bizThreadPool, closeTimeout);
        }

        //TODO add use common biz thread pool , otherwise use netty childEventLoop

        // if not use bizThreadPool , it was used netty childEventLoop
        // so until await childEventLoop grace shutdown
        log.info("service {} destroy down", serviceName);
    }
}
