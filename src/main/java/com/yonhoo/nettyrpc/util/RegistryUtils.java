package com.yonhoo.nettyrpc.util;

import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.logging.log4j.util.Strings;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RegistryUtils {
    private static String rootPathSuffix = "providers";

    public static boolean isRootPath(String providerPath) {
        return providerPath.endsWith(rootPathSuffix);
    }

    public static String getProviderPath(String providerPath) {
        String[] splitPaths = providerPath.split(rootPathSuffix + "/");
        try {
            return splitPaths[1];
        } catch (Exception e) {
            e.printStackTrace();
            throw RpcException.with(RpcErrorCode.NO_PROVIDER_PATH);
        }

    }

    public static Map<String, String> buildStringMapFromBytes(ChildData data) {
        if (Objects.isNull(data.getData())) {
            return Collections.emptyMap();
        }

        String dataStr = new String(data.getData());
        return Arrays.stream(URLDecoder.decode(dataStr, StandardCharsets.UTF_8).split("&"))
                .filter(Strings::isNotBlank)
                .map(item -> {
                    String[] keyAndValue = item.split("=");
                    return Pair.of(keyAndValue[0], keyAndValue[1]);
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public static String getRootPath(String providerPath) {
        String[] splitPaths = providerPath.split(rootPathSuffix + "/");
        try {
            return splitPaths[0] + rootPathSuffix + "/";
        } catch (Exception e) {
            e.printStackTrace();
            throw RpcException.with(RpcErrorCode.NO_PROVIDER_PATH);
        }

    }
}
