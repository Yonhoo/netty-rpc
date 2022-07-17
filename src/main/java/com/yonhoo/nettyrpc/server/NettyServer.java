package com.yonhoo.nettyrpc.server;

import com.google.common.base.Preconditions;
import com.yonhoo.nettyrpc.protocol.ProtocolNegotiator;
import com.yonhoo.nettyrpc.util.RuntimeUtil;
import com.yonhoo.nettyrpc.util.ThreadPoolFactoryUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;
import java.util.List;

public class NettyServer {

    private static final Logger log  = LogManager.getLogger(NettyServer.class.getName());

    private final List<? extends SocketAddress> addresses;
    private final ProtocolNegotiator protocolNegotiator;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
            RuntimeUtil.cpus() * 2,
            ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
    );

    public NettyServer(List<SocketAddress> listenAddresses) {
        this.addresses = Preconditions.checkNotNull(listenAddresses,"address");
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.protocolNegotiator = null;
    }


}
