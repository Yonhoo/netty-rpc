package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.common.Url;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionPool {
    //TODO separate executor
    private final EventExecutor executor;
    private final long acquireTimeoutNanos;
    private final AtomicInteger acquiredChannelCount;
    private boolean closed;
    private final AcquireTimeoutAction action;
    private final ConcurrentHashMap<Url, BasePool> urlPoolMap = new ConcurrentHashMap<>();
    private Bootstrap bootstrap;

    public ConnectionPool(Bootstrap bootstrap, int poolSize) {
        this(bootstrap, null, poolSize, -1);
    }

    public ConnectionPool(Bootstrap bootstrap, int poolSize, long acquireTimeoutMillis) {
        this(bootstrap, AcquireTimeoutAction.FAIL, poolSize, acquireTimeoutMillis);
    }

    public ConnectionPool(Bootstrap bootstrap, AcquireTimeoutAction action, int poolSize, long acquireTimeoutMillis) {
        this.bootstrap = bootstrap;
        this.acquiredChannelCount = new AtomicInteger();
        ObjectUtil.checkPositive(poolSize, "pool Size");
        this.action = action;
        if (action == null && acquireTimeoutMillis == -1L) {
            this.acquireTimeoutNanos = -1L;
        } else {
            if (action == null && acquireTimeoutMillis != -1L) {
                throw new NullPointerException("action");
            }

            if (action != null && acquireTimeoutMillis < 0L) {
                throw new IllegalArgumentException("acquireTimeoutMillis: " + acquireTimeoutMillis + " (expected: >= 0)");
            }

            this.acquireTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(acquireTimeoutMillis);
        }

        this.executor = bootstrap.config().group().next();
    }

    public int acquiredChannelCount() {
        return this.acquiredChannelCount.get();
    }

    public Future<Connection> acquireConnection(Url url) {
        Promise<Connection> promise = this.executor.newPromise();
        try {
            if (this.executor.inEventLoop()) {
                doRequireConnection(url, promise);
            } else {
                this.executor.execute(new Runnable() {
                    public void run() {
                        ConnectionPool.this.doRequireConnection(url, promise);
                    }
                });
            }
        } catch (Throwable var3) {
            promise.tryFailure(var3);
        }

        return promise;
    }

    private void doRequireConnection(Url url, Promise<Connection> promise) {
        BasePool basePool = urlPoolMap.get(url);
        if (basePool == null) {
            basePool = new BasePool(bootstrap, new ClientChannelPoolHandler(), 1, url);
            basePool.init();
            urlPoolMap.put(url, basePool);
        }

        this.acquire0(promise, basePool);
    }

    private void acquire0(Promise<Connection> promise, BasePool basePool) {
        try {
            assert this.executor.inEventLoop();

            if (this.closed) {
                promise.setFailure(new IllegalStateException("ChannelPool was closed"));
                return;
            }

            AcquireTask task = new AcquireTask(promise);
            if (this.action != null) {
                task.timeoutFuture = this.executor.schedule(new TimeoutTask(basePool, task, action),
                        this.acquireTimeoutNanos, TimeUnit.NANOSECONDS);
            }

            basePool.acquire(task.promise);

        } catch (Throwable var4) {
            promise.tryFailure(var4);
        }

    }

//    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
//        ObjectUtil.checkNotNull(promise, "promise");
//        Promise<Void> p = this.executor.newPromise();
//        super.release(channel, p.addListener(new FutureListener<Void>() {
//            public void operationComplete(Future<Void> future) {
//                try {
//                    assert ConnectionPool.this.executor.inEventLoop();
//
//                    if (ConnectionPool.this.closed) {
//                        channel.close();
//                        promise.setFailure(new IllegalStateException("ChannelPool was closed"));
//                        return;
//                    }
//
//                    if (future.isSuccess()) {
//                        promise.setSuccess(null);
//                    } else {
//                        promise.setFailure(future.cause());
//                    }
//                } catch (Throwable var3) {
//                    promise.tryFailure(var3);
//                }
//
//            }
//        }));
//        return promise;
//    }

    public void close() {
        try {
            this.closeAsync().await();
        } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(var2);
        }
    }

    public Future<Void> closeAsync() {
        if (this.executor.inEventLoop()) {
            return this.close0();
        } else {
            final Promise<Void> closeComplete = this.executor.newPromise();
            this.executor.execute(new Runnable() {
                public void run() {
                    close0().addListener(new FutureListener<Void>() {
                        public void operationComplete(Future<Void> f) throws Exception {
                            if (f.isSuccess()) {
                                closeComplete.setSuccess(null);
                            } else {
                                closeComplete.setFailure(f.cause());
                            }

                        }
                    });
                }
            });
            return closeComplete;
        }
    }

    private Future<Void> close0() {
        assert this.executor.inEventLoop();

        if (!this.closed) {
            this.closed = true;

            this.acquiredChannelCount.set(0);

            return GlobalEventExecutor.INSTANCE.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    urlPoolMap.values().forEach(BasePool::close);
                    return null;
                }
            });

        } else {
            return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
        }
    }

    private static final class AcquireTimeoutException extends TimeoutException {
        private AcquireTimeoutException() {
            super("Acquire operation took longer then configured maximum time");
        }

        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private class AcquireListener implements FutureListener<Connection> {
        private final Promise<Connection> originalPromise;
        private ScheduledFuture<?> timeoutFuture;

        AcquireListener(Promise<Connection> originalPromise) {
            this.originalPromise = originalPromise;
        }

        public void operationComplete(Future<Connection> future) {
            try {
                assert ConnectionPool.this.executor.inEventLoop();

                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                }

                if (ConnectionPool.this.closed) {
                    if (future.isSuccess()) {
                        (future.getNow()).close();
                    }

                    this.originalPromise.setFailure(new IllegalStateException("ChannelPool was closed"));
                    return;
                }

                if (future.isSuccess()) {
                    this.originalPromise.setSuccess(future.getNow());
                } else {
                    this.originalPromise.setFailure(future.cause());
                }
            } catch (Throwable var3) {
                this.originalPromise.tryFailure(var3);
            }

        }
    }

    private class TimeoutTask implements Runnable {
        private final AcquireTask acquireTask;
        private final AcquireTimeoutAction action;
        private final BasePool basePool;

        private TimeoutTask(BasePool basePool, AcquireTask acquireTask, AcquireTimeoutAction action) {
            this.basePool = basePool;
            this.acquireTask = acquireTask;
            this.action = action;
        }

        public final void run() {
            assert executor.inEventLoop();

            switch (action) {
                case FAIL:
                    acquireTask.promise.setFailure(new AcquireTimeoutException());
                    break;
                case NEW:
                    basePool.acquire(acquireTask.promise);
                    break;
                default:
                    throw new Error();
            }

        }
    }

    private final class AcquireTask extends AcquireListener {
        final Promise<Connection> promise;
        final long expireNanoTime;
        ScheduledFuture<?> timeoutFuture;

        AcquireTask(Promise<Connection> promise) {
            super(promise);
            this.expireNanoTime = System.nanoTime() + ConnectionPool.this.acquireTimeoutNanos;
            this.promise = ConnectionPool.this.executor.newPromise().addListener((FutureListener) this);
        }
    }

    public enum AcquireTimeoutAction {
        NEW,
        FAIL;

        AcquireTimeoutAction() {
        }
    }
}

