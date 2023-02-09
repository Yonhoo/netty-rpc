package com.yonhoo.nettyrpc.registry;

import java.util.HashMap;
import java.util.Map;

public class ZookeeperRegistryHelper {

    public static String buildProviderPath(String rootPath, ProviderConfig config) {
        return rootPath + "netty-rpc/" + config.getProviderName() + "/providers";
    }

    public static String convertMetaData(ProviderConfig providerConfig,
                                         ServiceConfig serviceConfig) {
        Map<String, String> providerMap = convertProviderToMap(providerConfig, serviceConfig);

        return convertMap2Pair(providerMap);
    }

    public static Map<String, String> convertProviderToMap(ProviderConfig providerConfig,
                                                           ServiceConfig serviceConfig) {
        HashMap<String, String> providerMap = new HashMap<>();
        providerMap.put("PROVIDER_NAME", providerConfig.getProviderName());
        providerMap.put("PROVIDER_PROTOCOL", providerConfig.getProtocol());
        providerMap.put("SERVICE_WEIGHT", String.valueOf(serviceConfig.getWeight()));

        return providerMap;
    }

    private static String convertMap2Pair(Map<String, String> map) {

        if (map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(128);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(getKeyPairs(entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }

    private static String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }

}
