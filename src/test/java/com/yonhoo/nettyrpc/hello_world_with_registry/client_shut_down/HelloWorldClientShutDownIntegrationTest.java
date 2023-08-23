package com.yonhoo.nettyrpc.hello_world_with_registry.client_shut_down;

import com.yonhoo.nettyrpc.client.DefaultConsumerBootstrap;
import com.yonhoo.nettyrpc.hello_world_with_registry.client_base.BaseIntegrationTest;
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

    @Test
    void should_client_side_grace_fully_shut_down_when_a_client_shut_down_signal_given_server_long_process() throws Exception {
        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

        HelloWorld helloWorld = (HelloWorld) config.refer();
        Future<String> res = executorService.submit(() -> helloWorld.doItDelay(10));
        Thread.sleep(2 * 1000);
        DefaultConsumerBootstrap.close();

        assertThat(res.get()).isEqualTo("sleep done");
    }
}
