package com.yonhoo.nettyrpc.client_consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.common.ApplicationContextUtil;
import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.registry.ConsumerConfig;
import com.yonhoo.nettyrpc.registry.ProviderConfig;
import com.yonhoo.nettyrpc.registry.ProviderInfo;
import com.yonhoo.nettyrpc.registry.ProviderInfoListener;
import com.yonhoo.nettyrpc.registry.ServiceConfig;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistry;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistryTest;
import com.yonhoo.nettyrpc.registry.base.BaseZkTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HelloWorldConsumerTest extends BaseZkTest {
    private static ZookeeperRegistry zookeeperRegistry;

    @BeforeAll
    static void setUp() {
        RegistryPropertiesConfig propertiesConfig = new RegistryPropertiesConfig();
        propertiesConfig.setAddress("127.0.0.1");
        propertiesConfig.setApplication("test-application");
        propertiesConfig.setPort(2181);

        zookeeperRegistry = new ZookeeperRegistry(propertiesConfig);

        ServiceConfig serviceConfig = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13456)
                .weight(0.7)
                .build();

        ProviderConfig providerConfig = ProviderConfig.builder()
                .providerName(HelloWorld.class.getName())
                .serviceConfigList(List.of(serviceConfig))
                .build();

        zookeeperRegistry.registry(providerConfig);
    }

    @Test
    void should_return_say_hi_when_new_hello_world_consumer_given_subscribe_port_13456_provider() throws Exception {

        try (MockedStatic<ApplicationContextUtil> utilities = Mockito.mockStatic(ApplicationContextUtil.class)) {
            utilities.when(() -> ApplicationContextUtil.getBean(ZookeeperRegistry.class.getName()))
                    .thenReturn(zookeeperRegistry);

            ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getName(), 300000, "async");

            HelloWorld helloWorld = (HelloWorld) config.refer();
            String hiSomething = helloWorld.sayHello("hi");

            assertThat(hiSomething).isEqualTo("yonhoo hi");
        }

    }
}
