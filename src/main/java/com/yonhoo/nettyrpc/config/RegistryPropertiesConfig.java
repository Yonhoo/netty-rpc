package com.yonhoo.nettyrpc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "registry")
@Component
@Getter
@Setter
public class RegistryPropertiesConfig {
    private String address;
    private Integer port;
    private String application;
}
