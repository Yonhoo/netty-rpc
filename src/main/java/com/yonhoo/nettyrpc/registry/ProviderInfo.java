package com.yonhoo.nettyrpc.registry;

import lombok.Data;

@Data
public class ProviderInfo {
    private String host;
    private int port;
    private String providerName;
}
