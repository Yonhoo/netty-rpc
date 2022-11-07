package com.yonhoo.nettyrpc.connection;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;

public class ClientChannelPoolHandler implements ChannelPoolHandler {
    @Override
    public void channelReleased(Channel channel) throws Exception {

    }

    @Override
    public void channelAcquired(Channel channel) throws Exception {

    }

    @Override
    public void channelCreated(Channel channel) throws Exception {

    }
}
