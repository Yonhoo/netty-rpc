package com.yonhoo.nettyrpc.load_balancer;

import com.yonhoo.nettyrpc.registry.ProviderInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultLoadBalancer implements LoadBalancer {
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    public ProviderInfo select(List<ProviderInfo> providerInfos) {
        if (providerInfos.size() <= atomicInteger.get()) {
            atomicInteger.set(0);
        }
        return providerInfos.get(atomicInteger.getAndIncrement() % providerInfos.size());
    }
}
