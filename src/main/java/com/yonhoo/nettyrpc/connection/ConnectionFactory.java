package com.yonhoo.nettyrpc.connection;

import com.yonhoo.nettyrpc.common.Url;
import com.yonhoo.nettyrpc.exception.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionFactory {
    private final Bootstrap bootstrap;

    public ConnectionFactory(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Connection createConnection(Url url) {
        ChannelFuture channelFuture = bootstrap
                .connect(url.getAddress(), url.getPort())
                .awaitUninterruptibly()
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("The netty client connected to {} successful!", bootstrap.config().remoteAddress());
                    } else {
                        throw new IllegalStateException("netty client start error: ", future.cause());
                    }
                });
        if (!channelFuture.isDone()) {
            String errMsg = "Create connection to " + bootstrap.config().remoteAddress() + " not done!";
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

        return new Connection(channelFuture.channel());
    }
}
