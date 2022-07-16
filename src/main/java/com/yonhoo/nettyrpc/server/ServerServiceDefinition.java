package com.yonhoo.nettyrpc.server;

import java.util.Map;

public class ServerServiceDefinition {
    private final String serverName;
//    private Object serverImpl;
    private final Map<String,ServerMethodDefinition> methodDefinitionMap;

    public ServerServiceDefinition(String serverName, Map<String, ServerMethodDefinition> methodDefinitionMap) {
        this.serverName = serverName;
        this.methodDefinitionMap = methodDefinitionMap;
    }
}
