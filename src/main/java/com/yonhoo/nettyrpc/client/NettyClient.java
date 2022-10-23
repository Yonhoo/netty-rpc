package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcMessageDecoder;
import com.yonhoo.nettyrpc.protocol.RpcMessageEncoder;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClient {
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private ConcurrentHashMap<Class<?>, RpcClientProxy> serviceProxyMap;
    private final AtomicInteger streamId = new AtomicInteger();
    private final NettyRpcClientHandler nettyRpcClientHandler = new NettyRpcClientHandler();

    public NettyClient(String host, int port) {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  The timeout period of the connection.
                //  If this time is exceeded or the connection cannot be established, the connection fails.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(host, port)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // If no data is sent to the server within 15 seconds, a heartbeat request is sent
                        p.addLast(new IdleStateHandler(0, 0, 15, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(nettyRpcClientHandler);
                    }
                });
    }

    public Channel connect() {

        this.channel = bootstrap.connect().awaitUninterruptibly()
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("The netty client connected to {} successful!", bootstrap.config().remoteAddress());
                    } else {
                        throw new IllegalStateException("netty client start error");
                    }
                }).channel();
        return channel;
    }

    public <T> T registerService(Class<T> classType) {
        return null;
    }

    public Object syncInvoke(RpcRequest request) {
        if (this.isAvailable()) {
            try {
                RpcMessage rpcMessage = RpcMessage.builder()
                        .messageType(RpcConstants.REQUEST_TYPE)
                        .requestId(streamId.getAndIncrement())
                        .codec(RpcConstants.PROTOCOL_DEFAULT_TYPE)
                        .compress(CompressTypeEnum.NONE.getCode())
                        .data(request)
                        .build();

                CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
                nettyRpcClientHandler.setStreamResponsePromise(rpcMessage.getRequestId(), responseFuture);
                //TODO add future listener handle
                this.channel.writeAndFlush(rpcMessage);
                return getResponse(responseFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.error("invoke service[{}] method[{}] send message error", request.getServiceName(), request.getMethodName(), e);
                throw RpcException.with(RpcErrorCode.RPC_INVOKE_METHOD_ERROR);
            }
        } else {
            log.error("invoke service[{}] method[{}] error", request.getServiceName(), request.getMethodName());
            throw RpcException.with(RpcErrorCode.RPC_CHANNEL_IS_NOT_ACTIVE);
        }
    }

    public Object syncInvoke(RpcRequest request, int timeout) {
        return null;
    }

    private Object getResponse(RpcResponse response) {
        if (response.isSuccess()) {
            return response.getData();
        }
        throw new RpcException(response.getMessage());
    }

    public void close() {
        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }

    private boolean isAvailable() {
        return this.channel.isOpen() && this.channel.isActive();
    }
}
