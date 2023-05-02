package com.yonhoo.nettyrpc.registry;

import java.util.concurrent.TimeUnit;

public interface Registry {

    void registry(ProviderConfig providerConfig);

    boolean unRegistry(ProviderConfig providerConfig);

    void subscribe(ConsumerConfig config) throws InterruptedException;

    void subscribe(ConsumerConfig config, long waitTime, TimeUnit timeUnit) throws InterruptedException;

    void unSubscribe(ConsumerConfig config);

    void destroy();
}
