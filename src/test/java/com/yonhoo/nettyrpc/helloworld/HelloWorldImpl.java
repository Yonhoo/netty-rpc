package com.yonhoo.nettyrpc.helloworld;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloWorldImpl implements HelloWorld {
    @Override
    public String sayHello(String message) {
        return "yonhoo " + message;
    }

    @Override
    public String doItDelay(int sec) throws InterruptedException {
        log.info("doItDelay start");
        Thread.sleep(sec * 1000);
        log.info("sleep done");
        return "sleep done";
    }

    @Override
    public void sayError() {
        throw new RuntimeException("say error");
    }
}
