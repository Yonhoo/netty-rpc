package com.yonhoo.nettyrpc.registry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceConfig {
    private String ip;
    private int port;
    private double weight;

    public String getUrl() {
        return ip + ":" + port;
    }
}
