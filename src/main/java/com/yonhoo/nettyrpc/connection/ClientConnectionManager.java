package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.common.Url;

public interface ClientConnectionManager {
    Connection getConnection(Url url);

    void close();
}
