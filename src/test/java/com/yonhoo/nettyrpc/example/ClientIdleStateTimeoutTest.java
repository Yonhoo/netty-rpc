package com.yonhoo.nettyrpc.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.server_base.BaseIntegrationTest;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.protocol.RpcRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class ClientIdleStateTimeoutTest extends BaseIntegrationTest {

    @BeforeEach
    public void startServer() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        ServerServiceDefinition helloWorldService =
                new ServerServiceDefinition(HelloWorld.class.getName(),
                        new HelloWorldImpl(),
                        HelloWorld.class,
                        1,
                        executorService,
                        10);

        NettyServer nettyServer = NettyServerBuilder.forAddress("127.0.0.1", 13456)
                .addService(helloWorldService)
                .build();

        Executors.newSingleThreadExecutor().submit(nettyServer::start);
        Thread.sleep(2 * 1000);
    }

    @Test
    public void test() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);
        Connection connection = new Connection(nettyClient.getBootstrap()
                .connect().awaitUninterruptibly().channel());


        Thread.sleep(16 * 1000);

        RpcRequest request = RpcRequest.builder()
                .methodName("sayHello")
                .paramTypes(new Class[]{String.class})
                .parameters(new String[]{"weclome!"})
                .serviceName(HelloWorld.class.getName())
                .build();

        RpcException rpcException = Assertions.assertThrows(RpcException.class, () -> nettyClient.syncInvoke(request, connection));

        assertThat(rpcException.getErrorMessage()).isEqualTo("connect channel is not active");
        nettyClient.close();

    }
}
