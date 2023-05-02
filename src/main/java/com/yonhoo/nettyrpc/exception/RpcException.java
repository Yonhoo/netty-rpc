package com.yonhoo.nettyrpc.exception;

public class RpcException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public RpcException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public RpcException(String msg) {
        super(msg);
        this.errorCode = "-1";
        this.errorMessage = msg;
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "-1";
        this.errorMessage = message;
    }

    public static RpcException with(RpcErrorCode rpcErrorCode) {
        return new RpcException(rpcErrorCode.getCode(), rpcErrorCode.getMessage());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
