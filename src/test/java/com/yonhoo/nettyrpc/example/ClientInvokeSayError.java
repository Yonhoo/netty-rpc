package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;

public class ClientInvokeSayError {
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);
        Connection connection = new Connection(nettyClient.getBootstrap()
                .connect().awaitUninterruptibly().channel());
        RpcRequest request = RpcRequest.builder()
                .methodName("sayError")
                .serviceName(HelloWorld.class.getName())
                .build();

        String response = (String) nettyClient.syncInvoke(request, connection);
        System.out.println(response);

        nettyClient.close();
    }
}
