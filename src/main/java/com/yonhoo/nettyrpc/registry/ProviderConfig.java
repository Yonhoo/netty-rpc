package com.yonhoo.nettyrpc.registry;

import java.util.concurrent.ThreadPoolExecutor;
import lombok.Data;

@Data
public class ProviderConfig {
    private String providerName;
    private String protocol;
    private ThreadPoolExecutor executor;
}
