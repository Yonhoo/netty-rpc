package com.yonhoo.nettyrpc.client;

public interface ConsumerBootstrap<T> {
    T refer() throws Exception;
}
