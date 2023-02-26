package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.RpcConstants;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.logging.log4j.util.Strings;

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

    public void updateProvider(ConsumerConfig config, String providerPath, ChildData data) {
        log.info("interface {} get provider update info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        //notifyListener
        ProviderInfo providerInfo = buildProviderInfoByBytes(data, providerPath, data.getPath());

        notifyListenerForUpdate(providerInfo);
    }

    private ProviderInfo buildProviderInfoByBytes(ChildData data, String rootPath, String servicePath) {
        Map<String, String> dataMap = buildStringMapFromBytes(data);

        return ProviderInfo.builder()
                .rootPath(rootPath)
                .servicePath(servicePath)
                .providerName(dataMap.getOrDefault(RpcConstants.PROVIDER_NAME, null))
                .weight(Double.parseDouble(dataMap.getOrDefault(RpcConstants.SERVICE_WEIGHT, "0.0")))
                .build();
    }

    private Map<String, String> buildStringMapFromBytes(ChildData data) {
        if (Objects.nonNull(data.getData())) {
            return Collections.emptyMap();
        }

        String dataStr = new String(data.getData());
        return Arrays.stream(URLDecoder.decode(dataStr).split("&"))
                .filter(Strings::isNotBlank)
                .map(item -> {
                    String[] keyAndValue = item.split("=");
                    return Pair.of(keyAndValue[0], keyAndValue[1]);
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private void notifyListenerForUpdate(ProviderInfo providerInfo) {
        providerListenerMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(providerInfoListener -> providerInfoListener.updateProvider(providerInfo));
    }

    private void notifyListenerForCreate(ProviderInfo providerInfo) {
        providerListenerMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(providerInfoListener -> providerInfoListener.addProvider(providerInfo));
    }

    private void notifyListenerForRemove(ProviderInfo providerInfo) {
        providerListenerMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(providerInfoListener -> providerInfoListener.removeProvider(providerInfo));
    }

    public void removeProvider(ConsumerConfig config, String providerPath, ChildData data) {
        log.info("interface {} get provider remove info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        //notifyListener
        ProviderInfo providerInfo = buildProviderInfoByBytes(data, providerPath, data.getPath());

        notifyListenerForRemove(providerInfo);
    }

    public void addProvider(ConsumerConfig config, String providerPath, ChildData data) {

        log.info("interface {} get provider add info: path({}) ， data : [{}] , stat[{}]",
                config.getConsumerInterface(),
                data.getPath(),
                new String(data.getData(), StandardCharsets.UTF_8),
                data.getStat());

        try {
            ProviderInfo providerInfo = buildProviderInfoByBytes(data, providerPath, data.getPath());
            notifyListenerForCreate(providerInfo);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }

        System.out.println("dasdasd");

    }


}
