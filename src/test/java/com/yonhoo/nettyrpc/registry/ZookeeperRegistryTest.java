package com.yonhoo.nettyrpc.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.helloworld.HelloWorld;
import com.yonhoo.nettyrpc.registry.base.BaseZkTest;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class ZookeeperRegistryTest extends BaseZkTest {
    private static ZookeeperRegistry zookeeperRegistry;

    @BeforeAll
    static void setUp() {
        RegistryPropertiesConfig propertiesConfig = new RegistryPropertiesConfig();
        propertiesConfig.setAddress("127.0.0.1");
        propertiesConfig.setApplication("test-application");
        propertiesConfig.setPort(2181);

        zookeeperRegistry = new ZookeeperRegistry(propertiesConfig);
    }

    @AfterEach
    void tearUp() throws Exception {
        String providerPath = ZookeeperRegistryHelper.buildProviderPath("test-application/",
                HelloWorld.class.getSimpleName());
        zookeeperRegistry.getZkClient()
                .delete()
                .deletingChildrenIfNeeded()
                .forPath(providerPath);
    }

    @Test
    void should_get_registry_provider_data_when_call_registry_given_provider_service_config() throws Exception {
        //given
        ServiceConfig serviceConfig1 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13456)
                .weight(0.3)
                .build();

        ServiceConfig serviceConfig2 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13457)
                .weight(0.7)
                .build();

        ProviderConfig providerConfig = ProviderConfig.builder()
                .providerName(HelloWorld.class.getSimpleName())
                .serviceConfigList(List.of(serviceConfig1, serviceConfig2))
                .build();

        //when
        zookeeperRegistry.registry(providerConfig);


        //then
        String providerPath = ZookeeperRegistryHelper.buildProviderPath("test-application/",
                providerConfig.getProviderName());

        CuratorFramework zkClient = zookeeperRegistry.getZkClient();

        String serverPath = providerPath + "/" + serviceConfig1.getUrl();

        String data = new String(zkClient.getData().forPath(serverPath));

        List<String> dataList = Arrays.stream(URLDecoder.decode(data).split("&"))
                .filter(Strings::isNotBlank)
                .collect(Collectors.toList());

        assertThat(dataList.get(0)).isEqualTo("SERVICE_WEIGHT=0.3");
        assertThat(dataList.get(1)).isEqualTo("PROVIDER_NAME=HelloWorld");

        serverPath = providerPath + "/" + serviceConfig2.getUrl();

        data = new String(zkClient.getData().forPath(serverPath));

        dataList = Arrays.stream(URLDecoder.decode(data).split("&"))
                .filter(Strings::isNotBlank)
                .collect(Collectors.toList());

        assertThat(dataList.get(0)).isEqualTo("SERVICE_WEIGHT=0.7");
        assertThat(dataList.get(1)).isEqualTo("PROVIDER_NAME=HelloWorld");
    }

    @Test
    void should_delete_provider_service_when_call_unregistry_given_provider_service_config() throws Exception {
        //given
        ServiceConfig serviceConfig1 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13456)
                .weight(0.3)
                .build();

        ServiceConfig serviceConfig2 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13457)
                .weight(0.7)
                .build();

        ProviderConfig providerConfig = ProviderConfig.builder()
                .providerName(HelloWorld.class.getSimpleName())
                .serviceConfigList(List.of(serviceConfig1, serviceConfig2))
                .build();

        zookeeperRegistry.registry(providerConfig);

        //when
        providerConfig.setServiceConfigList(List.of(serviceConfig2));
        zookeeperRegistry.unRegistry(providerConfig);

        //then
        String providerPath = ZookeeperRegistryHelper.buildProviderPath("test-application/",
                providerConfig.getProviderName());

        CuratorFramework zkClient = zookeeperRegistry.getZkClient();

        String serverPath = providerPath + "/" + serviceConfig1.getUrl();

        String data = new String(zkClient.getData().forPath(serverPath));

        List<String> dataList = Arrays.stream(URLDecoder.decode(data).split("&"))
                .filter(Strings::isNotBlank)
                .collect(Collectors.toList());

        assertThat(dataList.get(0)).isEqualTo("SERVICE_WEIGHT=0.3");
        assertThat(dataList.get(1)).isEqualTo("PROVIDER_NAME=HelloWorld");

        serverPath = providerPath + "/" + serviceConfig2.getUrl();

        Stat stat = zkClient.checkExists().forPath(serverPath);
        assertThat(stat).isNull();
    }

    @Test
    void should_get_all_added_service_list_when_subscribe_provider_given_new_provider_service() throws Exception {
        //given
        ServiceConfig serviceConfig1 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13456)
                .weight(0.3)
                .build();

        ServiceConfig serviceConfig2 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13457)
                .weight(0.7)
                .build();

        ProviderConfig providerConfig = ProviderConfig.builder()
                .providerName(HelloWorld.class.getSimpleName())
                .serviceConfigList(List.of(serviceConfig1, serviceConfig2))
                .build();

        zookeeperRegistry.registry(providerConfig);

        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getSimpleName(), 300000, "async");

        TestProviderInfoListener testProviderInfoListener = new TestProviderInfoListener(2);
        config.setProviderInfoListener(testProviderInfoListener);

        //when
        zookeeperRegistry.subscribe(config);

        testProviderInfoListener.getLatch().await();
        List<ProviderInfo> providerInfos = testProviderInfoListener.getProviderInfos();
        assertThat(providerInfos).hasSize(2);
    }

    @Test
    void should_get_all_rest_of_service_list_when_unsubscribe_provider_given_new_provider_service() throws Exception {
        //given
        ServiceConfig serviceConfig1 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13453)
                .weight(0.3)
                .build();

        ServiceConfig serviceConfig2 = ServiceConfig.builder()
                .ip("127.0.0.1")
                .port(13454)
                .weight(0.7)
                .build();

        ProviderConfig providerConfig = ProviderConfig.builder()
                .providerName(HelloWorld.class.getSimpleName())
                .serviceConfigList(List.of(serviceConfig1, serviceConfig2))
                .build();

        zookeeperRegistry.registry(providerConfig);

        ConsumerConfig config = new ConsumerConfig(HelloWorld.class.getSimpleName(), 300000, "async");

        TestProviderInfoListener testProviderInfoListener = new TestProviderInfoListener(2);
        config.setProviderInfoListener(testProviderInfoListener);

        //when
        zookeeperRegistry.subscribe(config);

        //then
        String providerPath = ZookeeperRegistryHelper.buildProviderPath("test-application/",
                providerConfig.getProviderName());

        testProviderInfoListener.getLatch().await();
        List<ProviderInfo> providerInfos = testProviderInfoListener.getProviderInfos();
        assertThat(providerInfos).hasSize(2);
        List<String> servicePathList = providerInfos.stream()
                .map(ProviderInfo::getServicePath)
                .collect(Collectors.toList());
        assertThat(servicePathList).contains(providerPath + "/" + serviceConfig1.getUrl(),
                providerPath + "/" + serviceConfig2.getUrl());

        testProviderInfoListener.setLatch(new CountDownLatch(1));

        providerConfig.setServiceConfigList(List.of(serviceConfig2));

        zookeeperRegistry.unRegistry(providerConfig);
        testProviderInfoListener.getLatch().await();

        providerInfos = testProviderInfoListener.getProviderInfos();
        assertThat(providerInfos).hasSize(1);
        assertThat(providerInfos.get(0).getServicePath()).isEqualTo(providerPath + "/" + serviceConfig1.getUrl());

    }

    private class TestProviderInfoListener implements ProviderInfoListener {
        private ConcurrentHashMap<String, ProviderInfo> providerInfos = new ConcurrentHashMap<>();
        private CountDownLatch latch;

        public TestProviderInfoListener(int nums) {
            latch = new CountDownLatch(nums);
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void addProvider(ProviderInfo providerInfo) {
            if (providerInfo.isProviderPath()) {
                return;
            }
            providerInfos.put(providerInfo.getServicePath(), providerInfo);
            latch.countDown();
        }

        @Override
        public void removeProvider(ProviderInfo providerInfo) {
            providerInfos.remove(providerInfo.getServicePath());
            latch.countDown();
        }

        @Override
        public void updateProvider(ProviderInfo providerInfo) {
            providerInfos.put(providerInfo.getServicePath(), providerInfo);
            latch.countDown();
        }

        public List<ProviderInfo> getProviderInfos() {
            return new ArrayList<>(providerInfos.values());
        }
    }
}
