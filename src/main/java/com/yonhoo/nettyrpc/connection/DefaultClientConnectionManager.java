package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.exception.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultClientConnectionManager implements ClientConnectionManager {
    private ConnectionPool connectionPool;
    private final Bootstrap bootstrap;
    private final int connectionPoolSize;

    public DefaultClientConnectionManager(Bootstrap bootstrap, int connectionPoolSize) {
        this.bootstrap = bootstrap;
        this.connectionPoolSize = connectionPoolSize;
        this.connectionPool = new ConnectionPool(bootstrap, connectionPoolSize);
    }

    public void startUp() {
        connectionPool.init();
    }

    public Connection getConnection() {
        Future<Connection> connectionFuture = connectionPool.acquireConnection().awaitUninterruptibly();
        if (connectionFuture.isSuccess()) {
            return connectionFuture.getNow();
        }
        log.error("get connection error", connectionFuture.cause());
        throw new RpcException(connectionFuture.cause().getMessage());
    }
}
