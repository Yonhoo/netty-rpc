package com.yonhoo.nettyrpc.protocol;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String serviceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;

    public String getRpcServiceName() {
        return this.serviceName + this.version;
    }
}
