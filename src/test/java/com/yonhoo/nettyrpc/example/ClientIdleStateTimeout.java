package com.yonhoo.nettyrpc.example;

import com.yonhoo.nettyrpc.client.NettyClient;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientIdleStateTimeout {
    public static void main(String[] args) {
        NettyClient nettyClient = new NettyClient("0.0.0.0", 13456);

        try {
            Thread.sleep(40 * 1000);

            RpcRequest request = RpcRequest.builder()
                    .methodName("sayHello")
                    .paramTypes(new Class[] {String.class})
                    .parameters(new String[] {"weclome!"})
                    .serviceName(HelloWorld.class.getName())
                    .build();

            String response = (String) nettyClient.syncInvoke(request);
            System.out.println(response);

        } catch (Exception exception) {
            log.error("client invoke error ", exception);
        }

        nettyClient.close();
    }
}
