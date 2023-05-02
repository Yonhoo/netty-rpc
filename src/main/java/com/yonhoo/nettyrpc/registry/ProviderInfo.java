package com.yonhoo.nettyrpc.registry;

import com.yonhoo.nettyrpc.common.Url;
import java.net.MalformedURLException;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderInfo {
    private String address;
    private Integer port;
    private String providerName;
    private Double weight;
    private String rootPath;
    private String servicePath;

    public boolean isProviderPath() {
        return rootPath.equals(servicePath);
    }

    public Url getUrl() {
        return new Url(address, port);
    }
}
