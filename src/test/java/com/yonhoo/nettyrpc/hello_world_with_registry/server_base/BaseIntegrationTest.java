package com.yonhoo.nettyrpc.hello_world_with_registry.server_base;

import com.yonhoo.nettyrpc.registry.base.BaseZkTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = NettyApplicationTest.class)
public abstract class BaseIntegrationTest extends BaseZkTest {
}
