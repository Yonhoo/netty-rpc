package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.RpcConstants;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;

public class ZookeeperRegistryHelper {

    public static String buildProviderPath(String rootPath, String providerPath) {
        return rootPath + "netty-rpc/" + providerPath + "/providers";
    }

    public static String convertMetaData(ProviderConfig providerConfig,
                                         ServiceConfig serviceConfig) {
        Map<String, String> providerMap = convertProviderToMap(providerConfig, serviceConfig);

        return convertMap2Pair(providerMap);
    }

    public static Map<String, String> convertMetaDataToMap(String metaData) {
        return Arrays.stream(metaData.split("&"))
                .map(entry -> {
                    String[] keyValue = entry.split("=");
                    if (Strings.EMPTY.equals(keyValue[0]) || Strings.EMPTY.equals(keyValue[1])) {
                        return null;
                    }
                    return Pair.of(keyValue[0], keyValue[1]);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public static Map<String, String> convertProviderToMap(ProviderConfig providerConfig,
                                                           ServiceConfig serviceConfig) {
        HashMap<String, String> providerMap = new HashMap<>();
        providerMap.put(RpcConstants.PROVIDER_NAME, providerConfig.getProviderName());
        providerMap.put(RpcConstants.SERVICE_WEIGHT, String.valueOf(serviceConfig.getWeight()));

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
            return "&" + key + "=" + value;
        } else {
            return "";
        }
    }

}
