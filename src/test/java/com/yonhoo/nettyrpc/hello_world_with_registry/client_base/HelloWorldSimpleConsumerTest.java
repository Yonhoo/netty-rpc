package com.yonhoo.nettyrpc.hello_world_with_registry.client_base;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import org.junit.jupiter.api.Test;

public class HelloWorldSimpleConsumerTest extends BaseIntegrationTest {

    @Test
    void should_return_say_hi_when_new_hello_world_consumer_given_subscribe_port_13456_provider() throws Exception {
        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

        HelloWorld helloWorld = (HelloWorld) config.refer();
        String hiSomething = helloWorld.sayHello("hi");

        assertThat(hiSomething).isEqualTo("yonhoo hi");
    }
}
