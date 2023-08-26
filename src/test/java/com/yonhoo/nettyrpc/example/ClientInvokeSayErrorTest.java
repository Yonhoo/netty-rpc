package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.server_base.BaseIntegrationTest;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClientInvokeSayErrorTest extends BaseIntegrationTest {

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
    public void test() {
        NettyClient nettyClient = new NettyClient("127.0.0.1", 13456);
        Connection connection = new Connection(nettyClient.getBootstrap()
                .connect().awaitUninterruptibly().channel());
        RpcRequest request = RpcRequest.builder()
                .methodName("sayError")
                .serviceName(HelloWorld.class.getName())
                .build();

        RpcException rpcException = assertThrows(RpcException.class, () -> nettyClient.syncInvoke(request, connection));
        assertThat(rpcException.getErrorMessage()).isEqualTo("say error");

        nettyClient.close();
    }
}
