package com.yonhoo.nettyrpc.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Connection {
    private Channel channel;
    private final ConcurrentHashMap<Integer, Promise> invokePromiseMap =
            new ConcurrentHashMap<>();
    private AtomicInteger referenceCount = new AtomicInteger();
    private AtomicBoolean closed = new AtomicBoolean(false);

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

    public Connection(Channel channel) {
        this.channel = channel;
        this.channel.attr(CONNECTION).set(this);
    }

    public boolean isFine() {
        return this.channel != null && this.channel.isActive();
    }

    public Promise addInvokeFuture(Integer invokeId, Promise future) {
        Promise origin = this.invokePromiseMap.putIfAbsent(invokeId, future);
        if (origin == null) {
            this.referenceCount.incrementAndGet();
        }
        return origin;
    }

    public Promise removeInvokeFuture(Integer invokeId) {
        Promise result = this.invokePromiseMap.remove(invokeId);
        if (result != null) {
            this.referenceCount.decrementAndGet();
        }
        return result;
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
        Iterator<Map.Entry<Integer, Promise>> iter = invokePromiseMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, Promise> entry = iter.next();
            iter.remove();
            Promise future = entry.getValue();
            if (future != null) {
                future.setFailure(new Throwable("connection closed"));
            }
        }
    }

}
