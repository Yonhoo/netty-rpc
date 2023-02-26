package com.yonhoo.nettyrpc.registry.base;

import java.io.IOException;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseZkTest {
    protected static TestingServer server = null;

    @BeforeAll
    public static void beforeSetUp() {
        try {
            server = new TestingServer(2181, true);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void afterDestroy() {
        if (server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
