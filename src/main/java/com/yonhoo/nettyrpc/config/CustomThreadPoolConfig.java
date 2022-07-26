package com.yonhoo.nettyrpc.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class CustomThreadPoolConfig {
    /**
     * executor parameters
     */
    public static final int DEFAULT_CORE_POOL_SIZE = 8;
    public static final int DEFAULT_MAXIMUM_POOL_SIZE_SIZE = 100;
    public static final int DEFAULT_KEEP_ALIVE_TIME = 60000;
    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
    public static final int DEFAULT_BLOCKING_QUEUE_CAPACITY = 100;
    public static final int BLOCKING_QUEUE_CAPACITY = 100;

    /**
     * build ThreadPoolExecutor
     *
     * @param corePoolSize    executor core pool size
     * @param maximumPoolSize executor maximum pool size
     * @param keepAliveTime   thread alive time
     * @param queueSize       set queue size
     * @return ThreadPoolExecutor
     */
    public static ThreadPoolExecutor initPool(int corePoolSize, int maximumPoolSize,
                                              long keepAliveTime, Integer queueSize) {
        BlockingQueue<Runnable> poolQueue = (queueSize != null && queueSize > 0)
                ? new LinkedBlockingQueue<>(queueSize) : new SynchronousQueue<>();

        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                TimeUnit.MILLISECONDS, poolQueue);
    }
}
