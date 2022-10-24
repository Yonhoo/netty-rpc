package com.yonhoo.nettyrpc.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

public class DefaultClientConnectionManager {
    private ConnectionPool connectionPool;
    private final Bootstrap bootstrap;
    private final int connectionPoolSize;

    public DefaultClientConnectionManager(Bootstrap bootstrap, int connectionPoolSize) {
        this.bootstrap = bootstrap;
        this.connectionPoolSize = connectionPoolSize;
    }

    public void startUp() {
        connectionPool = new ConnectionPool(bootstrap, connectionPoolSize);
    }

    public Connection getConnection() {
        return null;
    }
}
