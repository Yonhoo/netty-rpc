package com.yonhoo.nettyrpc.protocol;

import com.yonhoo.nettyrpc.exception.RpcException;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 715745410605631233L;
    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = -1;
    public static final int SUCCESS = 0;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private Object data;

    public boolean isSuccess() {
        return code == 0;
    }

    public static RpcResponse success(Object data) {
        RpcResponse response = new RpcResponse();
        response.setCode(SUCCESS_CODE);
        response.setMessage("The remote call is successful");
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static RpcResponse fail(Integer errorCode, String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setCode(errorCode);
        response.setMessage(errorMessage);
        return response;
    }

    public Object getData() {
        if (code == SUCCESS) {
            return data;
        }
        throw new RpcException(message);
    }

}
