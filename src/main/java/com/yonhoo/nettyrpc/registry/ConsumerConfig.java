package com.yonhoo.nettyrpc.registry;

import lombok.Data;

@Data
public class ConsumerConfig {
    private String consumerName;
    private String timeout;
    private String invokeType;
    private String protocol;
}
