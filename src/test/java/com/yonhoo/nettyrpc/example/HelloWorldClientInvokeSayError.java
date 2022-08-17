package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import io.netty.channel.Channel;

public class HelloWorldClientInvokeSayError {
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient();
        Channel channel = nettyClient.connect("0.0.0.0", 13456);

        if (channel.isActive()) {

            RpcRequest request = RpcRequest.builder()
                    .methodName("sayError")
                    .serviceName(HelloWorld.class.getName())
                    .build();

            String response = (String) nettyClient.syncInvoke(request);
            System.out.println(response);
        }

        nettyClient.close();
    }
}
