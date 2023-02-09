package com.yonhoo.nettyrpc.registry;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.Data;

@Data
public class ProviderConfig {
    private String providerName;
    private String protocol;
    private List<ServiceConfig> serviceConfigList;
    private boolean registered;
    private ThreadPoolExecutor executor;
}
