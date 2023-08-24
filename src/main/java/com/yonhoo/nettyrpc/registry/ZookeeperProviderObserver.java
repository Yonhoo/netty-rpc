package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.RpcConstants;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.yonhoo.nettyrpc.util.RegistryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;

@Slf4j
public class ZookeeperProviderObserver {

    public static final String PROVIDERS = "providers";
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
        Map<String, String> dataMap = RegistryUtils.buildStringMapFromBytes(data);
        String portWithAddressStr = servicePath.substring(servicePath.indexOf(PROVIDERS) +
                PROVIDERS.length() + 1);

        String[] portWithAddress = portWithAddressStr.split(":");

        return ProviderInfo.builder()
                .rootPath(rootPath)
                .servicePath(servicePath)
                .port(Integer.valueOf(portWithAddress[1]))
                .address(portWithAddress[0])
                .providerName(dataMap.getOrDefault(RpcConstants.PROVIDER_NAME, null))
                .weight(Double.parseDouble(dataMap.getOrDefault(RpcConstants.SERVICE_WEIGHT, "0.0")))
                .build();
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

        if (providerPath.equals(data.getPath())) {
            log.info("filter root provider path {}", providerPath);
            return;
        }
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

    }


}
