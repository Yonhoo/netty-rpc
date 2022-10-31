package com.yonhoo.nettyrpc.connection;

public interface ClientConnectionManager {
    Connection getConnection();

    void startUp();
}
