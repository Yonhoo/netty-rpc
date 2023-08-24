package com.yonhoo.nettyrpc.server;

import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;


class ServerServiceDefinitionUnitTest {

    @Test
    void should_destroy_biz_pool_waiting_for_thread_complete_before_timeout_when_invoke_destroy_given_biz_pool_and_run_workers() {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        ServerServiceDefinition helloWorldService =
                new ServerServiceDefinition(HelloWorld.class.getName(),
                        new HelloWorldImpl(),
                        HelloWorld.class,
                        1,
                        executorService,
                        10);

        AtomicBoolean complete = new AtomicBoolean(false);

        executorService.submit(() -> {
            try {
                Thread.sleep(9 * 1000);
                complete.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //when
        helloWorldService.destroy();

        //then
        assertThat(complete).isTrue();
    }

    @Test
    void should_destroy_biz_pool_after_timeout_when_invoke_destroy_given_biz_pool_and_run_workers() {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        ServerServiceDefinition helloWorldService =
                new ServerServiceDefinition(HelloWorld.class.getName(),
                        new HelloWorldImpl(),
                        HelloWorld.class,
                        1,
                        executorService,
                        10);

        AtomicBoolean complete = new AtomicBoolean(false);

        executorService.submit(() -> {
            try {
                Thread.sleep(11 * 1000);
                complete.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //when
        helloWorldService.destroy();

        //then
        assertThat(complete).isFalse();
    }

}