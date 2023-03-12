package com.yonhoo.nettyrpc.load_balancer;

import com.yonhoo.nettyrpc.registry.ProviderInfo;
import java.util.List;

public interface LoadBalancer {
    ProviderInfo select(List<ProviderInfo> providerInfos);
}
