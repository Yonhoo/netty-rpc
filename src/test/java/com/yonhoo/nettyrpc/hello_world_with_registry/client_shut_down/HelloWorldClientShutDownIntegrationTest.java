package com.yonhoo.nettyrpc.hello_world_with_registry.client_shut_down;

import com.yonhoo.nettyrpc.client.DefaultConsumerBootstrap;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import com.yonhoo.nettyrpc.server_base.BaseIntegrationTest;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * need run Server Main first at com.yonhoo.nettyrpc.hello_world_with_registry.server_base
 */

public class HelloWorldClientShutDownIntegrationTest extends BaseIntegrationTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    ServerServiceDefinition helloWorldService =
            new ServerServiceDefinition(HelloWorld.class.getName(),
                    new HelloWorldImpl(),
                    HelloWorld.class,
                    1,
                    executorService,
                    10);

    NettyServer nettyServer = NettyServerBuilder.forAddress("127.0.0.1", 13460)
            .addService(helloWorldService)
            .build();

    @Test
    void should_client_side_grace_fully_shut_down_when_a_client_shut_down_signal_given_server_long_process() throws Exception {
        Executors.newSingleThreadExecutor().submit(nettyServer::start);
        Thread.sleep(2 * 1000);
        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

        HelloWorld helloWorld = (HelloWorld) config.refer();
        Future<String> res = executorService.submit(() -> helloWorld.doItDelay(10));
        Thread.sleep(2 * 1000);
        DefaultConsumerBootstrap.close();

        assertThat(res.get()).isEqualTo("sleep done");
    }
}
