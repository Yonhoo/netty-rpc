package com.yonhoo.nettyrpc.registry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderInfo {
    private String address;
    private int port;
    private String providerName;
    private int weight;
}
