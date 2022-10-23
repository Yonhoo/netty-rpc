package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientIdleStateTimeout {
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);
        Channel channel = nettyClient.connect();

        try {
            Thread.sleep(40 * 1000);
            if (channel.isActive()) {
                RpcRequest request = RpcRequest.builder()
                        .methodName("sayHello")
                        .paramTypes(new Class[] {String.class})
                        .parameters(new String[] {"weclome!"})
                        .serviceName(HelloWorld.class.getName())
                        .build();

                String response = (String) nettyClient.syncInvoke(request);
                System.out.println(response);
            } else {
                log.error("connection close");
            }
        } catch (Exception exception) {
            log.error("client invoke error ", exception);
        }

        nettyClient.close();
    }
}
