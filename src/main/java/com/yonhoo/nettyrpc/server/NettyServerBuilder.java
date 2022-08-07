package com.yonhoo.nettyrpc.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class NettyServerBuilder extends ServerBuilder<NettyServerBuilder> {

    private final SocketAddress listenAddresse;
    private final ConcurrentHashMap<String, ServerServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();
    private final Map<Object, Object> channelOptionals;
    private final Map<Object, Object> childChannelOptionals;
    private final ServerConfig serverConfig = new ServerConfig();

    @Override
    public NettyServerBuilder forPort(int port) {
        return forAddress(new InetSocketAddress(port));
    }

    private NettyServerBuilder forAddress(InetSocketAddress address) {
        return new NettyServerBuilder(address);
    }

    private NettyServerBuilder(SocketAddress address) {
        this.channelOptionals = new HashMap<>();
        this.childChannelOptionals = new HashMap<>();
        this.listenAddresse = address;
    }

    @Override
    public NettyServerBuilder addService(ServerServiceDefinition service) {
        serviceDefinitionMap.put(service.getServiceName(), service);
        return this;
    }

    /**
     * time unit MILLISECONDS
     *
     * @param connectAliveTime keep alive time when build connection
     * @return NettyServerBuilder
     */
    @Override
    public NettyServerBuilder keepAliveTime(long connectAliveTime) {
        serverConfig.setConnAliveTime(connectAliveTime);
        return this;
    }

    /**
     * build biz pool
     * <p>
     * time unit MILLISECONDS
     *
     * @param corePoolSize    executor core pool size
     * @param maximumPoolSize executor maximum pool size
     * @param keepAliveTime   thread alive time
     * @return NettyServerBuilder
     */
    @Override
    public NettyServerBuilder bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        serverConfig.setCorePoolSize(corePoolSize);
        serverConfig.setMaximumPoolSize(maximumPoolSize);
        serverConfig.setThreadAliveTime(keepAliveTime);
        return this;
    }

    /**
     * build biz pool
     * <p>
     * time unit MILLISECONDS
     *
     * @param corePoolSize    executor core pool size
     * @param maximumPoolSize executor maximum pool size
     * @param keepAliveTime   thread alive time
     * @param queueSize       queue size
     * @return NettyServerBuilder
     */
    @Override
    public NettyServerBuilder bizPoolConfig(int corePoolSize, int maximumPoolSize, long keepAliveTime, int queueSize) {
        serverConfig.setCorePoolSize(corePoolSize);
        serverConfig.setMaximumPoolSize(maximumPoolSize);
        serverConfig.setThreadAliveTime(keepAliveTime);
        serverConfig.setQueueSize(queueSize);
        return this;
    }

    @Override
    public Object build() {
        return new NettyServer(listenAddresse, serverConfig, serviceDefinitionMap);
    }

}
