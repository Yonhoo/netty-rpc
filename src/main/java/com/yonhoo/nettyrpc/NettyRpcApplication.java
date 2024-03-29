package com.yonhoo.nettyrpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NettyRpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(NettyRpcApplication.class, args);
    }

}
