package com.yonhoo.nettyrpc.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

public class BasePool implements ChannelPool {
    private static final AttributeKey<BasePool> POOL_KEY = AttributeKey.newInstance("io.netty.channel.pool.BasePool");
    //FIFO
    private final Deque<Channel> channelDequeue;
    private final ChannelPoolHandler handler;
    private final ChannelHealthChecker healthCheck;
    private final Bootstrap bootstrap;

    public BasePool(Bootstrap bootstrap, ChannelPoolHandler handler) {
        this(bootstrap, handler, ChannelHealthChecker.ACTIVE);
    }

    public BasePool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthCheck) {
        this.channelDequeue = new ArrayDeque<>();
        this.handler = ObjectUtil.checkNotNull(handler, "handler");
        this.healthCheck = ObjectUtil.checkNotNull(healthCheck, "healthCheck");
        this.bootstrap = (ObjectUtil.checkNotNull(bootstrap, "bootstrap")).clone();
        this.bootstrap.handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                assert ch.eventLoop().inEventLoop();

                handler.channelCreated(ch);
            }
        });
    }

    protected Bootstrap bootstrap() {
        return this.bootstrap;
    }

    protected ChannelPoolHandler handler() {
        return this.handler;
    }

    protected ChannelHealthChecker healthChecker() {
        return this.healthCheck;
    }

    public final Future<Channel> acquire() {
        return this.acquire(this.bootstrap.config().group().next().newPromise());
    }

    public Future<Channel> acquire(Promise<Channel> promise) {
        return acquireHealthyFromPoolOrNew(ObjectUtil.checkNotNull(promise, "promise"));
    }

    private Future<Channel> acquireHealthyFromPoolOrNew(Promise<Channel> promise) {
        try {
            final Channel ch = this.pollChannel();
            if (ch == null) {
                Bootstrap bs = this.bootstrap.clone();
                bs.attr(POOL_KEY, this);
                ChannelFuture f = this.connectChannel(bs);
                if (f.isDone()) {
                    this.notifyConnect(f, promise);
                    // new channel offer into dequeue
                    if (promise.isSuccess()) {
                        channelDequeue.offer(ch);
                    }
                } else {
                    f.addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                            notifyConnect(future, promise);
                            // new channel offer into dequeue
                            if (promise.isSuccess()) {
                                channelDequeue.offer(ch);
                            }
                        }
                    });
                }
            } else {
                channelDequeue.offer(ch);
                EventLoop loop = ch.eventLoop();
                if (loop.inEventLoop()) {
                    doHealthCheck(ch, promise);
                } else {
                    loop.execute(new Runnable() {
                        public void run() {
                            doHealthCheck(ch, promise);
                        }
                    });
                }
            }
        } catch (Throwable var5) {
            promise.tryFailure(var5);
        }

        return promise;
    }

    private void notifyConnect(ChannelFuture future, Promise<Channel> promise) {
        Channel channel = null;

        try {
            if (future.isSuccess()) {
                channel = future.channel();
                this.handler.channelAcquired(channel);
                if (!promise.trySuccess(channel)) {
                    this.release(channel);
                }
            } else {
                promise.tryFailure(future.cause());
            }
        } catch (Throwable var5) {
            this.closeAndFail(channel, var5, promise);
        }

    }

    private void doHealthCheck(final Channel channel, final Promise<Channel> promise) {
        try {
            assert channel.eventLoop().inEventLoop();

            Future<Boolean> f = this.healthCheck.isHealthy(channel);
            if (f.isDone()) {
                notifyHealthCheck(f, channel, promise);
            } else {
                f.addListener((FutureListener<Boolean>) future -> notifyHealthCheck(future, channel, promise));
            }
        } catch (Throwable var4) {
            this.closeAndFail(channel, var4, promise);
        }

    }

    private void notifyHealthCheck(Future<Boolean> future, Channel channel, Promise<Channel> promise) {
        try {
            assert channel.eventLoop().inEventLoop();

            if (future.isSuccess() && future.getNow()) {
                channel.attr(POOL_KEY).set(this);
                this.handler.channelAcquired(channel);
                promise.setSuccess(channel);
            } else {
                this.closeChannel(channel);
                this.acquireHealthyFromPoolOrNew(promise);
            }
        } catch (Throwable var5) {
            this.closeAndFail(channel, var5, promise);
        }

    }

    protected ChannelFuture connectChannel(Bootstrap bs) {
        return bs.connect();
    }

    public final Future<Void> release(Channel channel) {
        return this.release(channel, channel.eventLoop().newPromise());
    }

    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        try {
            ObjectUtil.checkNotNull(channel, "channel");
            ObjectUtil.checkNotNull(promise, "promise");
            EventLoop loop = channel.eventLoop();
            if (loop.inEventLoop()) {
                this.doReleaseChannel(channel, promise);
            } else {
                loop.execute(() -> BasePool.this.doReleaseChannel(channel, promise));
            }
        } catch (Throwable var4) {
            this.closeAndFail(channel, var4, promise);
        }

        return promise;
    }

    private void doReleaseChannel(Channel channel, Promise<Void> promise) {
        try {
            assert channel.eventLoop().inEventLoop();

            if (channel.attr(POOL_KEY).getAndSet(null) != this) {
                this.closeAndFail(channel, new IllegalArgumentException("Channel " + channel + " was not acquired from this ChannelPool"),
                        promise);
            }

            this.doHealthCheckOnRelease(channel, promise);
        } catch (Throwable var4) {
            this.closeAndFail(channel, var4, promise);
        }

    }

    private void doHealthCheckOnRelease(final Channel channel, final Promise<Void> promise) {
        final Future<Boolean> f = this.healthCheck.isHealthy(channel);
        if (f.isDone()) {
            this.releaseAndOfferIfHealthy(channel, promise, f);
        } else {
            f.addListener((FutureListener<Boolean>) future -> BasePool.this.releaseAndOfferIfHealthy(channel, promise, f));
        }

    }

    private void releaseAndOfferIfHealthy(Channel channel, Promise<Void> promise, Future<Boolean> future) {
        try {
            if (future.getNow()) {
                this.releaseAndOffer(channel, promise);
            } else {
                this.handler.channelReleased(channel);
                promise.setSuccess(null);
            }
        } catch (Throwable var5) {
            this.closeAndFail(channel, var5, promise);
        }

    }

    private void releaseAndOffer(Channel channel, Promise<Void> promise) throws Exception {
        if (this.offerChannel(channel)) {
            this.handler.channelReleased(channel);
            promise.setSuccess(null);
        } else {
            this.closeAndFail(channel, new BasePool.ChannelPoolFullException(), promise);
        }

    }

    private void closeChannel(Channel channel) {
        channel.attr(POOL_KEY).getAndSet(null);
        channel.close();
    }

    private void closeAndFail(Channel channel, Throwable cause, Promise<?> promise) {
        if (channel != null) {
            try {
                this.closeChannel(channel);
            } catch (Throwable var5) {
                promise.tryFailure(var5);
            }
        }

        promise.tryFailure(cause);
    }

    protected Channel pollChannel() {
        return this.channelDequeue.pollFirst();
    }

    protected boolean offerChannel(Channel channel) {
        return this.channelDequeue.offer(channel);
    }

    public void close() {
        while (true) {
            Channel channel = this.pollChannel();
            if (channel == null) {
                return;
            }

            channel.close().awaitUninterruptibly();
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
