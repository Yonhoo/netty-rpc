package com.yonhoo.nettyrpc.server;

import java.lang.reflect.Method;

public final class ServerMethodDefinition {
    private final String methodName;
    private final Method handler;

    public ServerMethodDefinition(String methodName, Method handler) {
        this.methodName = methodName;
        this.handler = handler;
    }
}
