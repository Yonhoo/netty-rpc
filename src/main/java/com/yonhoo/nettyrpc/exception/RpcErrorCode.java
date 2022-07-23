package com.yonhoo.nettyrpc.exception;

public enum RpcErrorCode {
    SERVICE_NOT_THIS_METHOD("10001", "service not this method");

    private final String code;
    private final String message;

    RpcErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
