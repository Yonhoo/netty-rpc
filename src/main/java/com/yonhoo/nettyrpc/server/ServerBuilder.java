package com.yonhoo.nettyrpc.server;

public abstract class ServerBuilder<T extends ServerBuilder<T>> {

    public abstract T forPort(int port);

    public abstract T addService(ServerServiceDefinition service);

    public T intercept(Object interceptor) {
        throw new UnsupportedOperationException();
    }

    public T addTransportFilter(Object filter) {
        throw new UnsupportedOperationException();
    }

    public abstract Object build();

    public abstract T keepAliveTime(long aliveMilliSeconds);

    public abstract T bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime);

    public abstract T bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                    int queueSize);

}
