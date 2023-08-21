package com.yonhoo.nettyrpc.helloworld;

public interface HelloWorld {
    String sayHello(String message);

    String doItDelay(int sec) throws InterruptedException;

    void sayError();
}
