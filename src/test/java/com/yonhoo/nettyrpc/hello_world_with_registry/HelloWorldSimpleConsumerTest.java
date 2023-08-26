package com.yonhoo.nettyrpc.hello_world_with_registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import com.yonhoo.nettyrpc.server_base.BaseIntegrationTest;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloWorldSimpleConsumerTest extends BaseIntegrationTest {

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    ServerServiceDefinition helloWorldService =
            new ServerServiceDefinition(HelloWorld.class.getName(),
                    new HelloWorldImpl(),
                    HelloWorld.class,
                    1,
                    executorService,
                    10);

    NettyServer nettyServer = NettyServerBuilder.forAddress("127.0.0.1", 13459)
            .addService(helloWorldService)
            .build();


    @Test
    void should_return_say_hi_when_new_hello_world_consumer_given_subscribe_port_13456_provider() throws Exception {
        Executors.newSingleThreadExecutor().submit(nettyServer::start);
        Thread.sleep(2 * 1000);

        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

        HelloWorld helloWorld = (HelloWorld) config.refer();
        String hiSomething = helloWorld.sayHello("hi");

        assertThat(hiSomething).isEqualTo("yonhoo hi");
    }
}
