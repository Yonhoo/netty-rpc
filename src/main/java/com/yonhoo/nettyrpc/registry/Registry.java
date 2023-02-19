package com.yonhoo.nettyrpc.registry;

import java.util.List;

public interface Registry {

    void registry(ProviderConfig providerConfig);

    boolean unRegistry(ProviderConfig providerConfig);

    List<ProviderInfo> subscribe(ConsumerConfig config);

    void unSubscribe(ConsumerConfig config);

    void destroy();
}
