package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.registry.ProviderInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvokeContext {
    private ProviderInfo providerInfo;
}
