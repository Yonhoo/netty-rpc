package com.yonhoo.nettyrpc.common;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RpcRunTimeContext {
    public static final String DEFAULT_STOP_TIME_OUT = "10";
    public static String STOP_TIME_OUT = "stopTimeOut";

    private static final Map<String, String> rpcContextConfig = new HashMap<>();

    /**
     * graceful shut down time out , unit second
     */
    public static Integer stopTimeOut() {
        return Integer.valueOf(Optional.ofNullable(rpcContextConfig.get(STOP_TIME_OUT))
                .orElse(DEFAULT_STOP_TIME_OUT));
    }

    public static void putAttribute(String attributeName, String attributeValue, String defaultValue) {
        rpcContextConfig.put(attributeName, attributeValue == null ? defaultValue : attributeValue);
    }
}
