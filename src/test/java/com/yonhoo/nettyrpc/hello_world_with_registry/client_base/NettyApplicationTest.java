package com.yonhoo.nettyrpc.hello_world_with_registry.client_base;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
@ComponentScans(value = {
        @ComponentScan(basePackages = {
                "com.yonhoo.nettyrpc.*"
        }
        )
})
public class NettyApplicationTest {
    public static void main(String[] args) {

    }
}
