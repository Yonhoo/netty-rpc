package com.yonhoo.nettyrpc.hello_world_with_registry.server_shut_down;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.common.RpcRunTimeContext;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.server_base.BaseIntegrationTest;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class HelloWorldServerShutDown3Test extends BaseIntegrationTest {

    private static final ServerServiceDefinition helloWorldService =
            new ServerServiceDefinition(HelloWorld.class.getName(),
                    new HelloWorldImpl(),
                    HelloWorld.class,
                    1,
                    null,
                    10);

    private final NettyServer nettyServer = NettyServerBuilder.forAddress("127.0.0.1", 13457)
            .addService(helloWorldService)
            .build();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    void should_shut_down_gracefully_after_consume_finished_when_process_client_request_and_server_shut_down_time_out_given_hello_call_server_shut_down_signal() throws Exception {

        //given
        executorService.submit(nettyServer::start);
        Thread.sleep(2 * 1000);

        RpcRunTimeContext.putAttribute(RpcRunTimeContext.STOP_TIME_OUT, "3", "3");
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13457);
        Connection connection = new Connection(nettyClient.getBootstrap()
                .connect().awaitUninterruptibly().channel());
        RpcRequest request = RpcRequest.builder()
                .methodName("doItDelay")
                .paramTypes(new Class[]{int.class})
                .parameters(new Object[]{10})
                .serviceName(HelloWorld.class.getName())
                .build();

        long start = System.currentTimeMillis();

        Future<Object> responseFuture = executorService.submit(() -> nettyClient.syncInvoke(request, connection));

        Thread.sleep(10);
        nettyServer.destroy();

        RpcException rpcException = assertThrows(RpcException.class, () -> nettyClient.syncInvoke(request, connection));
        assertThat(rpcException.getErrorMessage()).isEqualTo("invoke method error");

        String response = (String) responseFuture.get(1, TimeUnit.SECONDS);

        assertThat(response).isEqualTo("sleep done");
        long end = System.currentTimeMillis();

        assertThat(end - start).isGreaterThan(10 * 1000);
        nettyClient.close();
    }


}
