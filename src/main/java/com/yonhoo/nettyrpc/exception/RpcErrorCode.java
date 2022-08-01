package com.yonhoo.nettyrpc.exception;

public enum RpcErrorCode {
    SERVICE_NOT_THIS_METHOD("10001", "service not this method"),
    CODEC_NOT_SUPPORTED_THIS_TYPE("10002", "service codec type not supported"),
    CHANNEL_READ_IS_NOT_REQUEST_TYPE("10003", "service channel read request message type not supported"),
    SERVICE_NOT_REGISTERED("10004", "this service not registered");

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
