package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;

public class ClientInvokeSayHi {
    public static void main(String[] args) throws InterruptedException {
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);

            RpcRequest request = RpcRequest.builder()
                    .methodName("sayHello")
                    .paramTypes(new Class[] {String.class})
                    .parameters(new String[] {"weclome!"})
                    .serviceName(HelloWorld.class.getName())
                    .build();

            String response = (String) nettyClient.syncInvoke(request);
            System.out.println(response);
        Thread.sleep(10000);
        nettyClient.close();
    }
}
