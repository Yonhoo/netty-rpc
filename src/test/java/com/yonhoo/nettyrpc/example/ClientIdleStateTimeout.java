package com.yonhoo.nettyrpc.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientIdleStateTimeout {
    public static void main(String[] args) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);
        Connection connection = new Connection(nettyClient.getBootstrap()
                .connect().awaitUninterruptibly().channel());

        try {
            Thread.sleep(16 * 1000);

            RpcRequest request = RpcRequest.builder()
                    .methodName("sayHello")
                    .paramTypes(new Class[] {String.class})
                    .parameters(new String[] {"weclome!"})
                    .serviceName(HelloWorld.class.getName())
                    .build();

            String response = (String) nettyClient.syncInvoke(request, connection);
            System.out.println(response);

        } catch (RpcException exception) {
            countDownLatch.countDown();
            log.error("client invoke error ", exception);
        }

        assertThat(countDownLatch.getCount()).isZero();
        nettyClient.close();
    }
}
