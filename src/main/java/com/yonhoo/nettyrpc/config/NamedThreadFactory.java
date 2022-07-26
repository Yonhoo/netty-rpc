package com.yonhoo.nettyrpc.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_COUNT = new AtomicInteger();
    private final AtomicInteger threadCount = new AtomicInteger(1);
    private static final String FIRST_PREFIX = "YRPC";
    private final String namePrefix;
    private final ThreadFactory backingThreadFactory;
    private final boolean isDaemon;

    public NamedThreadFactory(String secondPrefix, boolean daemon) {
        namePrefix = FIRST_PREFIX + secondPrefix + "-" + POOL_COUNT.getAndIncrement() + "-T";
        this.backingThreadFactory = Executors.defaultThreadFactory();
        isDaemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = backingThreadFactory.newThread(r);

        thread.setName(namePrefix + threadCount.getAndIncrement());

        thread.setDaemon(isDaemon);

        return thread;
    }
}
