package com.yonhoo.nettyrpc.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool extends BasePool {
    private final EventExecutor executor;
    private final long acquireTimeoutNanos;
    private final Runnable timeoutTask;
    private final Queue<AcquireTask> pendingAcquireQueue;
    private final int maxConnections;
    private final int maxPendingAcquires;
    private final AtomicInteger acquiredChannelCount;
    private int pendingAcquireCount;
    private boolean closed;

    public ConnectionPool(Bootstrap bootstrap, ChannelPoolHandler handler, int maxConnections) {
        this(bootstrap, handler, maxConnections, Integer.MAX_VALUE);
    }

    public ConnectionPool(Bootstrap bootstrap, ChannelPoolHandler handler, int maxConnections, int maxPendingAcquires) {
        this(bootstrap, handler, AcquireTimeoutAction.FAIL, -1L, maxConnections, maxPendingAcquires);
    }

    public ConnectionPool(Bootstrap bootstrap, ChannelPoolHandler handler, AcquireTimeoutAction action, long acquireTimeoutMillis,
                          int maxConnections, int maxPendingAcquires) {
        super(bootstrap, handler);
        this.pendingAcquireQueue = new ArrayDeque();
        this.acquiredChannelCount = new AtomicInteger();
        ObjectUtil.checkPositive(maxConnections, "maxConnections");
        ObjectUtil.checkPositive(maxPendingAcquires, "maxPendingAcquires");
        if (action == null && acquireTimeoutMillis == -1L) {
            this.timeoutTask = null;
            this.acquireTimeoutNanos = -1L;
        } else {
            if (action == null && acquireTimeoutMillis != -1L) {
                throw new NullPointerException("action");
            }

            if (action != null && acquireTimeoutMillis < 0L) {
                throw new IllegalArgumentException("acquireTimeoutMillis: " + acquireTimeoutMillis + " (expected: >= 0)");
            }

            this.acquireTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(acquireTimeoutMillis);
            switch (action) {
                case FAIL:
                    this.timeoutTask = new TimeoutTask() {
                        public void onTimeout(AcquireTask task) {
                            task.promise.setFailure(new AcquireTimeoutException());
                        }
                    };
                    break;
                case NEW:
                    this.timeoutTask = new TimeoutTask() {
                        public void onTimeout(AcquireTask task) {
                            task.acquired();
                            ConnectionPool.super.acquire(task.promise);
                        }
                    };
                    break;
                default:
                    throw new Error();
            }
        }

        this.executor = bootstrap.config().group().next();
        this.maxConnections = maxConnections;
        this.maxPendingAcquires = maxPendingAcquires;
    }

    public int acquiredChannelCount() {
        return this.acquiredChannelCount.get();
    }

    public Future<Channel> acquire(final Promise<Channel> promise) {
        try {
            if (this.executor.inEventLoop()) {
                this.acquire0(promise);
            } else {
                this.executor.execute(new Runnable() {
                    public void run() {
                        ConnectionPool.this.acquire0(promise);
                    }
                });
            }
        } catch (Throwable var3) {
            promise.tryFailure(var3);
        }

        return promise;
    }

    private void acquire0(Promise<Channel> promise) {
        try {
            assert this.executor.inEventLoop();

            if (this.closed) {
                promise.setFailure(new IllegalStateException("ChannelPool was closed"));
                return;
            }

            if (this.acquiredChannelCount.get() < this.maxConnections) {
                assert this.acquiredChannelCount.get() >= 0;

                Promise<Channel> p = this.executor.newPromise();
                AcquireListener l = new AcquireListener(promise);
                l.acquired();
                p.addListener(l);
                super.acquire(p);
            } else {
                if (this.pendingAcquireCount >= this.maxPendingAcquires) {
                    this.tooManyOutstanding(promise);
                } else {
                    AcquireTask task = new AcquireTask(promise);
                    if (this.pendingAcquireQueue.offer(task)) {
                        ++this.pendingAcquireCount;
                        if (this.timeoutTask != null) {
                            task.timeoutFuture = this.executor.schedule(this.timeoutTask, this.acquireTimeoutNanos, TimeUnit.NANOSECONDS);
                        }
                    } else {
                        this.tooManyOutstanding(promise);
                    }
                }

                assert this.pendingAcquireCount > 0;
            }
        } catch (Throwable var4) {
            promise.tryFailure(var4);
        }

    }

    private void tooManyOutstanding(Promise<?> promise) {
        promise.setFailure(new IllegalStateException("Too many outstanding acquire operations"));
    }

    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        Promise<Void> p = this.executor.newPromise();
        super.release(channel, p.addListener(new FutureListener<Void>() {
            public void operationComplete(Future<Void> future) {
                try {
                    assert ConnectionPool.this.executor.inEventLoop();

                    if (ConnectionPool.this.closed) {
                        channel.close();
                        promise.setFailure(new IllegalStateException("ChannelPool was closed"));
                        return;
                    }

                    if (future.isSuccess()) {
                        ConnectionPool.this.decrementAndRunPendingAcquireTask();
                        promise.setSuccess(null);
                    } else {
                        Throwable cause = future.cause();
                        if (!(cause instanceof IllegalArgumentException)) {
                            ConnectionPool.this.decrementAndRunPendingAcquireTask();
                        }

                        promise.setFailure(future.cause());
                    }
                } catch (Throwable var3) {
                    promise.tryFailure(var3);
                }

            }
        }));
        return promise;
    }

    private void decrementAndRunPendingAcquireTask() {
        int currentCount = this.acquiredChannelCount.decrementAndGet();

        assert currentCount >= 0;

        this.runPendingAcquireTask();
    }

    private void runPendingAcquireTask() {
        while (true) {
            if (this.acquiredChannelCount.get() < this.maxConnections) {
                AcquireTask task = this.pendingAcquireQueue.poll();
                if (task != null) {
                    ScheduledFuture<?> timeoutFuture = task.timeoutFuture;
                    if (timeoutFuture != null) {
                        timeoutFuture.cancel(false);
                    }

                    --this.pendingAcquireCount;
                    task.acquired();
                    super.acquire(task.promise);
                    continue;
                }
            }

            assert this.pendingAcquireCount >= 0;

            assert this.acquiredChannelCount.get() >= 0;

            return;
        }
    }

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

            while (true) {
                AcquireTask task = this.pendingAcquireQueue.poll();
                if (task == null) {
                    this.acquiredChannelCount.set(0);
                    this.pendingAcquireCount = 0;
                    //TODO Why?
                    return GlobalEventExecutor.INSTANCE.submit(new Callable<Void>() {
                        public Void call() throws Exception {
                            ConnectionPool.super.close();
                            return null;
                        }
                    });
                }

                ScheduledFuture<?> f = task.timeoutFuture;
                if (f != null) {
                    f.cancel(false);
                }

                task.promise.setFailure(new ClosedChannelException());
            }
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

    private class AcquireListener implements FutureListener<Channel> {
        private final Promise<Channel> originalPromise;
        protected boolean acquired;

        AcquireListener(Promise<Channel> originalPromise) {
            this.originalPromise = originalPromise;
        }

        public void operationComplete(Future<Channel> future) {
            try {
                assert ConnectionPool.this.executor.inEventLoop();

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
                    if (this.acquired) {
                        ConnectionPool.this.decrementAndRunPendingAcquireTask();
                    } else {
                        ConnectionPool.this.runPendingAcquireTask();
                    }

                    this.originalPromise.setFailure(future.cause());
                }
            } catch (Throwable var3) {
                this.originalPromise.tryFailure(var3);
            }

        }

        public void acquired() {
            if (!this.acquired) {
                ConnectionPool.this.acquiredChannelCount.incrementAndGet();
                this.acquired = true;
            }
        }
    }

    private abstract class TimeoutTask implements Runnable {
        private TimeoutTask() {
        }

        public final void run() {
            assert executor.inEventLoop();

            long nanoTime = System.nanoTime();

            while (true) {
                AcquireTask task = pendingAcquireQueue.peek();
                if (task == null || nanoTime - task.expireNanoTime < 0L) {
                    return;
                }

                pendingAcquireQueue.remove();
                --pendingAcquireCount;
                this.onTimeout(task);
            }
        }

        public abstract void onTimeout(AcquireTask var1);
    }

    private final class AcquireTask extends AcquireListener {
        final Promise<Channel> promise;
        final long expireNanoTime;
        ScheduledFuture<?> timeoutFuture;

        AcquireTask(Promise<Channel> promise) {
            super(promise);
            this.expireNanoTime = System.nanoTime() + ConnectionPool.this.acquireTimeoutNanos;
            this.promise = ConnectionPool.this.executor.newPromise().addListener((FutureListener) this);
        }
    }

    public static enum AcquireTimeoutAction {
        NEW,
        FAIL;

        private AcquireTimeoutAction() {
        }
    }
}

