package com.yonhoo.nettyrpc.server;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServerConfig {
    private int corePoolSize;
    private int maximumPoolSize;
    private long connAliveTime;
    private long threadAliveTime;
    private Integer queueSize;
    private String registryAddress;
    private int registryPort;
    private Integer stopTimeOutSeconds;
}
