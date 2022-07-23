package com.yonhoo.nettyrpc.exception;

public class RpcException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public RpcException(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
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
