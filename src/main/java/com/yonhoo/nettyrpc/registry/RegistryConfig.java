package com.yonhoo.nettyrpc.registry;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RegistryConfig {
    private String address;
    private int port;
    private String rootPath;
    private int connectTimeout;
}
