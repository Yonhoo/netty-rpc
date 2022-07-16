package com.yonhoo.nettyrpc.server;

import org.springframework.lang.Nullable;

import java.util.concurrent.Executor;

public abstract class ServerBuilder<T extends ServerBuilder<T>> {

    public abstract T forPort(int port);

    public abstract T executor(@Nullable Executor executor);

    public abstract T addService(ServerServiceDefinition service);

    public T intercept(Object interceptor) {throw new UnsupportedOperationException();}

    public T addTransportFilter(Object filter) {throw new UnsupportedOperationException();}

    public abstract Object build();

}
