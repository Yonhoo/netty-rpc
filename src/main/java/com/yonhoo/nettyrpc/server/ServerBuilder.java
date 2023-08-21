package com.yonhoo.nettyrpc.server;

public interface ServerBuilder {

    ServerBuilder addService(ServerServiceDefinition service);

    ServerBuilder addTransportFilter(Object filter);

    ServerBuilder intercept(Object interceptor);

    Object build();

    ServerBuilder keepAliveTime(long aliveMilliSeconds);

    ServerBuilder bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime);

    ServerBuilder bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                int queueSize);

}
