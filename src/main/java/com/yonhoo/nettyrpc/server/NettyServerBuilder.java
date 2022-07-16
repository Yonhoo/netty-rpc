package com.yonhoo.nettyrpc.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public final class NettyServerBuilder extends ServerBuilder<NettyServerBuilder> {

    private static final Logger log = LogManager.getLogger(NettyServerBuilder.class.getName());

    private final List<SocketAddress> listenAddresses;
    private final Map<Object,Object> channelOptionals;
    private final Map<Object,Object> childChannelOptionals;
    private int maxMessageSize = 4194304;
    private Long keepAliveTimeInNanos = TimeUnit.HOURS.toNanos(2L);
    private Long keepAliveTimeoutInNanos = TimeUnit.SECONDS.toNanos(20L);

    @Override
    public NettyServerBuilder forPort(int port) {
        return forAddress(new InetSocketAddress(port));
    }

    private NettyServerBuilder forAddress(InetSocketAddress address) {
        return new NettyServerBuilder(address);
    }

    private NettyServerBuilder(SocketAddress address){
        this.channelOptionals = new HashMap<>();
        this.childChannelOptionals = new HashMap<>();
        this.listenAddresses = List.of(address);
    }

    @Override
    public NettyServerBuilder executor(Executor executor) {
        return null;
    }

    @Override
    public NettyServerBuilder addService(ServerServiceDefinition service) {
        return null;
    }

    @Override
    public Object build() {
        return new NettyServer(this.listenAddresses);
    }

}
