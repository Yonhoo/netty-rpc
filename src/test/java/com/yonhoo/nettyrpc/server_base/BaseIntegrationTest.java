package com.yonhoo.nettyrpc.server_base;

import com.yonhoo.nettyrpc.registry.base.BaseZkTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = NettyApplicationTest.class)
@TestPropertySource(locations="classpath:application.properties")
public abstract class BaseIntegrationTest extends BaseZkTest {
}
