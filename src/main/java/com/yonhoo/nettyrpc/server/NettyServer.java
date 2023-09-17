package com.yonhoo.nettyrpc.server;

import com.google.common.base.Preconditions;
import com.yonhoo.nettyrpc.common.Destroyable;
import com.yonhoo.nettyrpc.common.ExtensionLoader;
import com.yonhoo.nettyrpc.common.RpcRunTimeContext;
import com.yonhoo.nettyrpc.config.RegistryPropertiesConfig;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.protocol.RpcMessageDecoder;
import com.yonhoo.nettyrpc.protocol.RpcMessageEncoder;
import com.yonhoo.nettyrpc.registry.ProviderConfig;
import com.yonhoo.nettyrpc.registry.Registry;
import com.yonhoo.nettyrpc.registry.ServiceConfig;
import com.yonhoo.nettyrpc.registry.ZookeeperRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer implements Destroyable {
    private final InetSocketAddress address;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Registry registry;
    private final ServerConfig serverConfig;

    private final AtomicBoolean available = new AtomicBoolean(true);
    private final HashMap<String, ServerServiceDefinition> serviceDefinitionMap = new HashMap<>();

    public NettyServer(InetSocketAddress listenAddress, ServerConfig serverConfig,
                       List<ServerServiceDefinition> serviceDefinitionList) {
        this.address = Preconditions.checkNotNull(listenAddress, "address");
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.serverConfig = serverConfig;
        serviceDefinitionList.forEach(serverServiceDefinition ->
                serviceDefinitionMap.put(serverServiceDefinition.getServiceName(), serverServiceDefinition)
        );

        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy, "NettyRpcShutdownHook"));
    }

    public void start() {
        try {
            ChannelFuture f = serverStart();

            // registry service
            if (serviceDefinitionMap.values().isEmpty()) {
                throw RpcException.with(RpcErrorCode.SERVICE_IS_EMPTY);
            }

            registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension("registry");

            if (Objects.nonNull(registry)) {
                List<ProviderConfig> providerConfigs =
                        buildServiceConfig(new ArrayList<>(serviceDefinitionMap.values()));
                providerConfigs.forEach(registry::registry);
            }

            // wait for close
            f.channel().closeFuture().sync();
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("server start address [{}] error ", address.toString(), exception);
        } finally {
            log.info("shutdown bossGroup and workerGroup");
            destroy();
        }

    }

    private ChannelFuture serverStart() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_BACKLOG, 128)
                //ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcServerHandler(NettyServer.this));
                        // only for specific register service use pool
                        //p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                    }
                });
        log.info("worker event group threads {}", NettyRuntime.availableProcessors() * 2);
        // bind remote address
        return bootstrap.bind(this.address.getPort()).sync().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The netty server has connected [{}] successful!", address);
            } else {
                throw new IllegalStateException("netty server start error");
            }
        });
    }

    private List<ProviderConfig> buildServiceConfig(List<ServerServiceDefinition> serviceDefinitions) {
        return serviceDefinitions.stream()
                .map(service -> ProviderConfig.builder()
                        .providerName(service.getServiceName())
                        .serviceConfigList(Collections.singletonList(ServiceConfig.builder()
                                .weight(service.getWeight())
                                .port(address.getPort())
                                .ip(address.getHostName())
                                .build()))
                        .build())
                .collect(Collectors.toList());
    }

    public ServerServiceDefinition getServiceDefinitionByName(String serviceName) {
        return serviceDefinitionMap.get(serviceName);
    }

    @Override
    public void destroy() {
        if (!available.compareAndSet(true, false)) {
            return;
        }

        log.info("shut down gracefully start!!!");

        // 1. unregister service in registry center
        registry.destroy();

        // 2. waiting for all channel finish worker queues
        serviceDefinitionMap.values().forEach(ServerServiceDefinition::destroy);

        // 3. await for use worker thread to process requests
        Future<?> workerGroupShutDownedGracefully = workerGroup.shutdownGracefully();
        Future<?> bossGroupShutDownedGracefully = bossGroup.shutdownGracefully();

        Integer stopTimeOutSeconds = Optional.of(RpcRunTimeContext.stopTimeOut()).orElse(3);

        log.info("worker threads process done {}",
                workerGroupShutDownedGracefully.awaitUninterruptibly(stopTimeOutSeconds,
                        TimeUnit.SECONDS));
        bossGroupShutDownedGracefully.awaitUninterruptibly();
        log.info("Netty Rpc Server shut down");

    }
}
