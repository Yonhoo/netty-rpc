package com.yonhoo.nettyrpc.hello_world_with_registry.server_provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import org.junit.jupiter.api.Test;

public class ServerMain extends BaseIntegrationTest {

    @Test
    public void server_start() {
        ServerServiceDefinition helloWorldService =
                new ServerServiceDefinition(HelloWorld.class.getName(),
                        new HelloWorldImpl(),
                        HelloWorld.class,
                        1,
                        null);

        NettyServer nettyServer = NettyServerBuilder.forAddress("127.0.0.1", 13456)
                .addService(helloWorldService)
                .build();

        nettyServer.start();
    }

    @Test
    void should_return_say_hi_when_new_hello_world_consumer_given_subscribe_port_13456_provider() throws Exception {

        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

        HelloWorld helloWorld = (HelloWorld) config.refer();
        String hiSomething = helloWorld.sayHello("hi");

        assertThat(hiSomething).isEqualTo("yonhoo hi");

    }
}
