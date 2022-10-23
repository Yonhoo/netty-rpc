package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import io.netty.channel.Channel;

public class ClientInvokeSayError {
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);
        Channel channel = nettyClient.connect();

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
