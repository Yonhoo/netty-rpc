package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.common.Url;
import com.yonhoo.nettyrpc.exception.RpcException;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultClientConnectionManager implements ClientConnectionManager {
    private final ConnectionPool connectionPool;
    private static final NettyClient nettyClient = new NettyClient();

    public DefaultClientConnectionManager() {
        this.connectionPool = new ConnectionPool(nettyClient.getBootstrap(), 1);
    }

    @Override
    public Connection getConnection(Url url) {
        Future<Connection> connectionFuture = connectionPool.acquireConnection(url).awaitUninterruptibly();
        if (connectionFuture.isSuccess()) {
            return connectionFuture.getNow();
        }
        log.error("get connection error", connectionFuture.cause());
        throw new RpcException(connectionFuture.cause().getMessage());
    }

    @Override
    public void close() {
        log.info("connection manager start close");
        connectionPool.closeAwait();
        nettyClient.close();
    }
}
