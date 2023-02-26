package com.yonhoo.nettyrpc.registry;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.Builder;
import lombok.Data;


/*
  providerConfig currently provider service
 */

@Data
@Builder
public class ProviderConfig {
    private String providerName;
    private String protocol;
    private List<ServiceConfig> serviceConfigList;
    private boolean registered;
    private ThreadPoolExecutor executor;
}
