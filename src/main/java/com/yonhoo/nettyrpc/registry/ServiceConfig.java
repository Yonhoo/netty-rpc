package com.yonhoo.nettyrpc.registry;

import lombok.Data;

@Data
public class ServiceConfig {
    private String ip;
    private int port;
    private int weight;

    public String getUrl() {
        return ip + ":" + port;
    }
}
