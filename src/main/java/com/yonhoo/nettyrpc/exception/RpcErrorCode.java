package com.yonhoo.nettyrpc.exception;

public enum RpcErrorCode {
    SERVICE_NOT_THIS_METHOD("10001", "service not this method"),
    CODEC_NOT_SUPPORTED_THIS_TYPE("10002", "service codec type not supported"),
    CHANNEL_READ_IS_NOT_REQUEST_TYPE("10003", "service channel read request message type not supported"),
    SERVICE_NOT_REGISTERED("10004", "this service not registered"),
    RPC_INVOKE_METHOD_ERROR("10005", "invoke method error"),
    RPC_CHANNEL_IS_NOT_ACTIVE("10006", "connect channel is not active"),
    RPC_MESSAGE_TYPE_NOT_BE_EMPTY("10007", "rpc message type not be empty"),
    REGISTRY_CLIENT_START_EXCEPTION("10008", "registry client start exception"),
    REGISTRY_CLIENT_UNAVAILABLE("10009", "registry client unavailable"),
    REGISTRY_UNSUBSCRIBE_EXCEPTION("10010", "registry client unsubscribe exception"),
    SERVICE_IS_EMPTY("10011", "service list is empty"),
    NO_PROVIDER_PATH("10012", "service no provider path");

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
