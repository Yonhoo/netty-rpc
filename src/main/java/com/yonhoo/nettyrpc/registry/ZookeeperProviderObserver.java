package com.yonhoo.nettyrpc.registry;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;

@Slf4j
public class ZookeeperProviderObserver {

    private ConcurrentMap<ConsumerConfig, List<ProviderInfoListener>>
            providerListenerMap = new ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>>();

    public void addProviderListener(ConsumerConfig consumerConfig, ProviderInfoListener listener) {
        if (listener != null) {
            providerListenerMap.computeIfAbsent(consumerConfig, key ->
                    new CopyOnWriteArrayList<ProviderInfoListener>()).add(listener);
        }
    }

    public void removeProviderListener(ConsumerConfig consumerConfig) {
        providerListenerMap.remove(consumerConfig);
    }

    public void updateProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData) {
        log.info("interface {} get provider update info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        //notifyListener
    }

    public void removeProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData) {
        log.info("interface {} get provider remove info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        //notifyListener
    }

    public void addProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData) {
        log.info("interface {} get provider add info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        //notifyListener
    }



}
