package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Connection {
    private Channel channel;
    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> invokeFutureMap =
            new ConcurrentHashMap<>();
    private AtomicInteger referenceCount = new AtomicInteger();
    private AtomicBoolean closed = new AtomicBoolean(false);

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

    public Connection(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel not be null");
        Attribute<Connection> attr = this.channel.attr(CONNECTION);
        //.set(this);
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isFine() {
        return !closed.get() && this.channel != null && this.channel.isActive() && this.channel.isWritable();
    }

    public Boolean inEventLoop() {
        return channel.eventLoop().inEventLoop();
    }

    public void inEventLoopExecute(Runnable runnable) {
        channel.eventLoop().execute(runnable);
    }

    public CompletableFuture<RpcResponse> addInvokeFuture(Integer invokeId, CompletableFuture<RpcResponse> future) {
        if (!isFine()) {
            throw RpcException.with(RpcErrorCode.RPC_CHANNEL_IS_NOT_ACTIVE);
        }
        CompletableFuture<RpcResponse> origin = this.invokeFutureMap.putIfAbsent(invokeId, future);
        if (origin == null) {
            this.referenceCount.incrementAndGet();
        }
        return origin;
    }

    public CompletableFuture<RpcResponse> removeInvokeFuture(Integer invokeId) {
        CompletableFuture<RpcResponse> result = this.invokeFutureMap.remove(invokeId);
        if (result != null) {
            this.referenceCount.decrementAndGet();
        }
        return result;
    }

    public Integer getReferenceCount() {
        return referenceCount.get();
    }

    public void closeChannel() {
        try {
            if (!closed.get() && this.channel != null && referenceCount.get() == 0) {
                closed.compareAndSet(false, true);
                this.channel.close().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.info("Close the connection to remote address={}, result={}, cause={}",
                                Connection.this.channel.remoteAddress(), future.isSuccess(), future.cause());

                    }

                });
            }
        } catch (Exception e) {
            log.warn("Exception caught when closing connection {}",
                    Connection.this.channel.remoteAddress(), e);
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (this.channel != null) {
                    this.channel.close().addListener(new ChannelFutureListener() {

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            onClose();
                            log.info("Close the connection to remote address={}, result={}, cause={}",
                                    Connection.this.channel.remoteAddress(), future.isSuccess(), future.cause());

                        }

                    });
                }
            } catch (Exception e) {
                log.warn("Exception caught when closing connection {}",
                        Connection.this.channel.remoteAddress(), e);
            }
        }
    }

    private void onClose() {
        Iterator<Map.Entry<Integer, CompletableFuture<RpcResponse>>> iter = invokeFutureMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, CompletableFuture<RpcResponse>> entry = iter.next();
            iter.remove();
            CompletableFuture<RpcResponse> future = entry.getValue();
            if (future != null) {
                future.completeExceptionally(new Throwable("connection closed"));
            }
        }
    }

}
