package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.helloworld.HelloWorldImpl;
import com.yonhoo.nettyrpc.server.NettyServer;
import com.yonhoo.nettyrpc.server.NettyServerBuilder;
import com.yonhoo.nettyrpc.server.ServerServiceDefinition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceMain {
    public static void main(String[] args) {

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

        nettyServer.start();


    }
}
