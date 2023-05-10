package com.yonhoo.nettyrpc.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Socket;

@Slf4j
@Getter
public class NettyServerBuilder implements ServerBuilder {

    private final InetSocketAddress listenAddress;
    private final List<ServerServiceDefinition> serviceDefinitionList = new ArrayList<>();
    private final Map<Object, Object> channelOptionals;
    private final Map<Object, Object> childChannelOptionals;
    private final ServerConfig serverConfig = new ServerConfig();

    public static NettyServerBuilder forAddress(String address, int port) {
        return new NettyServerBuilder(new InetSocketAddress(address, port));
    }

    private NettyServerBuilder(InetSocketAddress address) {
        this.channelOptionals = new HashMap<>();
        this.childChannelOptionals = new HashMap<>();
        this.listenAddress = address;
    }

    @Override
    public NettyServerBuilder addService(ServerServiceDefinition service) {
        serviceDefinitionList.add(service);
        return this;
    }

    @Override
    public ServerBuilder addTransportFilter(Object filter) {
        throw new IllegalArgumentException();
    }

    @Override
    public ServerBuilder intercept(Object interceptor) {
        throw new IllegalArgumentException();
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
    public NettyServer build() {
        return new NettyServer(listenAddress, serverConfig, serviceDefinitionList);
    }

}
