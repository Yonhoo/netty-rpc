package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.exception.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionFactory {
    private final Bootstrap bootstrap;

    public ConnectionFactory(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Connection createConnection() {
        ChannelFuture channelFuture = bootstrap.connect();

        channelFuture.awaitUninterruptibly();
        if (!channelFuture.isDone()) {
            String errMsg = "Create connection to " + bootstrap.config().remoteAddress() + " timeout!";
            log.warn(errMsg);
            throw new RpcException(errMsg);
        }
        if (channelFuture.isCancelled()) {
            String errMsg = "Create connection to " + bootstrap.config().remoteAddress() + " cancelled by user!";
            log.warn(errMsg);
            throw new RpcException(errMsg);
        }
        if (!channelFuture.isSuccess()) {
            String errMsg = "Create connection to " + bootstrap.config().remoteAddress() + " error!";
            log.warn(errMsg);
            throw new RpcException(errMsg, channelFuture.cause());
        }
        Channel channel = channelFuture.channel();
        if (!channel.eventLoop().inEventLoop()) {
            channel.close();
            throw new RpcException("channel not in eventLoop");
        }

        return new Connection(channel);
    }
}
