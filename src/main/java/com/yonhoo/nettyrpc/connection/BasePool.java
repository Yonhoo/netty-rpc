package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.common.Url;
import com.yonhoo.nettyrpc.exception.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasePool {
    //FIFO
    private final Deque<Connection> connectionDequeue;
    private final ChannelPoolHandler handler;
    private final ChannelHealthChecker healthCheck;
    private final Bootstrap bootstrap;
    private final ConnectionFactory connectionFactory;
    private final int poolSize;
    private final Url url;

    public BasePool(Bootstrap bootstrap, ChannelPoolHandler handler, int poolSize, Url url) {
        this(bootstrap, handler, ChannelHealthChecker.ACTIVE, poolSize, url);
    }

    public BasePool(Bootstrap bootstrap, ChannelPoolHandler handler,
                    ChannelHealthChecker healthCheck, int poolSize, Url url) {
        this.poolSize = ObjectUtil.checkPositive(poolSize, "pool size should be greater than 0");
        this.bootstrap = (ObjectUtil.checkNotNull(bootstrap, "bootstrap")).clone();
        this.url = url;
        this.connectionFactory = new ConnectionFactory(this.bootstrap);
        this.connectionDequeue = new ArrayDeque<>();
        this.handler = ObjectUtil.checkNotNull(handler, "handler");
        this.healthCheck = ObjectUtil.checkNotNull(healthCheck, "healthCheck");
    }

    public void init() {
        for (int i = 0; i < poolSize; i++) {
            try {
                Connection connection = connectionFactory.createConnection(url);
                connectionDequeue.addLast(connection);
            } catch (RpcException e) {
                log.warn("connection pool init connection error ", e);
            }
        }
    }

    public Future<Connection> acquire(Promise<Connection> promise) {
        return acquireHealthyFromPoolOrNew(ObjectUtil.checkNotNull(promise, "promise"));
    }

    private Future<Connection> acquireHealthyFromPoolOrNew(Promise<Connection> promise) {
        try {
            final Connection connection = this.pollConnection();
            log.info("client connection {}", connection);
            if (connection == null && connectionDequeue.size() < poolSize) {
                log.info("create new connection when acquire empty connection from poll");
                Connection newConnection = connectionFactory.createConnection(url);
                if (newConnection.isFine()) {
                    this.notifyConnect(newConnection, promise);
                    // new connection offer into dequeue
                    if (promise.isSuccess()) {
                        connectionDequeue.offer(newConnection);
                    }
                } else {
                    newConnection.close();
                    return acquireHealthyFromPoolOrNew(promise);
                }
            } else {
                if (connection.inEventLoop()) {
                    doHealthCheck(connection, promise);
                } else {
                    connection.inEventLoopExecute(new Runnable() {
                        public void run() {
                            doHealthCheck(connection, promise);
                        }
                    });
                }
                connectionDequeue.offer(connection);
            }
        } catch (Throwable var5) {
            promise.tryFailure(var5);
        }

        return promise;
    }

    private void notifyConnect(Connection connection, Promise<Connection> promise) {

        try {
            Channel channel = connection.getChannel();
            this.handler.channelAcquired(channel);
            if (!promise.trySuccess(connection)) {
                this.release(connection);
            }
        } catch (Throwable var5) {
            this.closeAndFail(connection, var5, promise);
        }

    }

    private void doHealthCheck(final Connection connection, final Promise<Connection> promise) {

        assert connection.inEventLoop();
        if (connection.isFine()) {
            promise.setSuccess(connection);
        } else {
            connection.close();
            this.acquireHealthyFromPoolOrNew(promise);
        }
    }

    public final Future<Void> release(Connection connection) {
        return this.release(connection, connection.getChannel().eventLoop().newPromise());
    }

    public Future<Void> release(final Connection connection, final Promise<Void> promise) {
        try {
            ObjectUtil.checkNotNull(connection, "connection");
            ObjectUtil.checkNotNull(promise, "promise");
            if (connection.inEventLoop()) {
                this.doReleaseConnection(connection, promise);
            } else {
                connection.inEventLoopExecute(() -> BasePool.this.doReleaseConnection(connection, promise));
            }
        } catch (Throwable var4) {
            this.closeAndFail(connection, var4, promise);
        }

        return promise;
    }

    private void doReleaseConnection(Connection connection, Promise<Void> promise) {
        try {
            assert connection.inEventLoop();

            this.doHealthCheckOnRelease(connection, promise);
        } catch (Throwable var4) {
            this.closeAndFail(connection, var4, promise);
        }

    }

    private void doHealthCheckOnRelease(final Connection connection, final Promise<Void> promise) throws Exception {
        if (connection.isFine()) {
            this.releaseAndOfferIfHealthy(connection, promise);
        } else {
            this.closeAndFail(connection, new RpcException("connection is inactive"), promise);
            promise.setFailure(null);
        }

    }

    private void releaseAndOfferIfHealthy(Connection connection, Promise<Void> promise) {
        try {
            if (this.offerChannel(connection)) {
                this.handler.channelReleased(connection.getChannel());
                promise.setSuccess(null);
            }
        } catch (Exception e) {
            this.closeAndFail(connection, new BasePool.ChannelPoolFullException(), promise);
        }
    }

    private void closeAndFail(Connection connection, Throwable cause, Promise<?> promise) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable var5) {
                promise.tryFailure(var5);
            }
        }

        promise.tryFailure(cause);
    }

    private Connection pollConnection() {
        return this.connectionDequeue.pollFirst();
    }

    private boolean offerChannel(Connection channel) {
        return this.connectionDequeue.offer(channel);
    }

    public void close() {
        while (true) {
            Connection connection = this.pollConnection();
            if (connection == null) {
                return;
            }

            connection.close();
        }
    }

    public void closeAwait() {
        while (true) {
            Connection connection = this.pollConnection();
            if (connection.getReferenceCount() > 0) {
                connectionDequeue.offer(connection);
            } else if (connectionDequeue.isEmpty()) {
                connection.closeChannel();
                return;
            }
        }
    }

    public Future<Void> closeAsync() {
        return GlobalEventExecutor.INSTANCE.submit(new Callable<Void>() {
            public Void call() throws Exception {
                BasePool.this.close();
                return null;
            }
        });
    }

    private static final class ChannelPoolFullException extends IllegalStateException {
        private ChannelPoolFullException() {
            super("ChannelPool full");
        }

        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
