package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.client.DefaultConsumerBootstrap;
import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsumerConfig<T> {
    private String consumerInterface;
    private Integer timeout;
    private String invokeType;
    private String application;
    private DefaultConsumerBootstrap<T> consumerBootStrap;
    private ProviderInfoListener providerInfoListener;

    public ConsumerConfig(String consumerInterface, int timeout, String invokeType) {
        this.consumerInterface = consumerInterface;
        this.timeout = timeout;
        this.invokeType = invokeType;
    }

    public T refer() throws Exception {
        synchronized (this) {
            if (consumerBootStrap == null) {
                consumerBootStrap = new DefaultConsumerBootstrap(this);
            }
        }
        return consumerBootStrap.refer();
    }

}
