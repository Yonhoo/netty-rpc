package com.yonhoo.nettyrpc.server;

import com.google.common.base.Preconditions;
import com.yonhoo.nettyrpc.config.CustomThreadPoolConfig;
import com.yonhoo.nettyrpc.config.NamedThreadFactory;
import com.yonhoo.nettyrpc.protocol.ProtocolNegotiator;
import com.yonhoo.nettyrpc.util.RuntimeUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.List;

@Slf4j
public class NettyServer {
    private final List<? extends SocketAddress> addresses;
    private final ProtocolNegotiator protocolNegotiator;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private ThreadPoolExecutor bizThreadPool;
    private final ServerConfig serverConfig;

    public NettyServer(List<SocketAddress> listenAddresses, ServerConfig serverConfig) {
        this.addresses = Preconditions.checkNotNull(listenAddresses, "address");
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.protocolNegotiator = null;
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
}
