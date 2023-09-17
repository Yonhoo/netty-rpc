package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.SPI;

import java.util.concurrent.TimeUnit;

@SPI
public interface Registry {

    void registry(ProviderConfig providerConfig);

    boolean unRegistry(ProviderConfig providerConfig);

    void subscribe(ConsumerConfig config) throws InterruptedException;

    void subscribe(ConsumerConfig config, long waitTime, TimeUnit timeUnit) throws InterruptedException;

    void unSubscribe(ConsumerConfig config);

    void destroy();
}
