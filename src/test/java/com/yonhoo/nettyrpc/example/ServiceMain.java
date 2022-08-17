package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;

public class ServiceMain {
    public static void main(String[] args) {

        ServerServiceDefinition helloWorldService =
                new ServerServiceDefinition(HelloWorld.class.getName(),
                        new HelloWorldImpl(),
                        HelloWorld.class);

        NettyServer nettyServer = NettyServerBuilder.forPort(13456)
                .addService(helloWorldService)
                .build();

        nettyServer.start();


    }
}
