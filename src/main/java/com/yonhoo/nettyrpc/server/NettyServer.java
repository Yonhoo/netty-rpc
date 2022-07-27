package com.yonhoo.nettyrpc.server;

import com.google.common.base.Preconditions;
import com.yonhoo.nettyrpc.config.CustomThreadPoolConfig;
import com.yonhoo.nettyrpc.config.NamedThreadFactory;
import com.yonhoo.nettyrpc.protocol.RpcMessageDecoder;
import com.yonhoo.nettyrpc.protocol.RpcMessageEncode;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.List;

@Slf4j
public class NettyServer {
    private final List<? extends SocketAddress> addresses;
    private final RpcMessageEncode rpcMessageEncode;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private ThreadPoolExecutor bizThreadPool;
    private final ServerConfig serverConfig;

    public NettyServer(List<SocketAddress> listenAddresses, ServerConfig serverConfig) {
        this.addresses = Preconditions.checkNotNull(listenAddresses, "address");
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.rpcMessageEncode = null;
        this.serverConfig = serverConfig;
    }

    protected ThreadPoolExecutor initThreadPool() {
        ThreadPoolExecutor threadPool = CustomThreadPoolConfig.initPool(
                serverConfig.getCorePoolSize(),
                serverConfig.getMaximumPoolSize(),
                serverConfig.getThreadAliveTime(),
                serverConfig.getQueueSize());
        threadPool.setThreadFactory(new NamedThreadFactory(
                "SEVER-YL", false));
        return threadPool;
    }

    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        try {
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
                            // heartBeat
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncode());
                            p.addLast(new RpcMessageDecoder());
                            //p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
        } catch (Exception exception) {
            log.error("server start error ", exception);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
