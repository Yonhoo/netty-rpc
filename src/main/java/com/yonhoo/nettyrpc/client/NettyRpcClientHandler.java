package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;

import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        try {
            Channel channel = ctx.channel();
            Attribute<Connection> bindConnection = channel.attr(Connection.CONNECTION);
            Connection connection = bindConnection.get();
            CompletableFuture<RpcResponse> responseFuture = connection.removeInvokeFuture(rpcMessage.getRequestId());
            if (RpcConstants.RESPONSE_TYPE == rpcMessage.getMessageType()) {
                responseFuture.complete((RpcResponse) rpcMessage.getData());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            log.warn("this stream message was removed: {}", rpcMessage);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("channel exception caught", cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        //TODO add connect/disconnect event handler
        if (event instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) event).state();
            if (state == IdleState.ALL_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, event);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("channel closed");
        Channel channel = ctx.channel();
        Attribute<Connection> bindConnection = channel.attr(Connection.CONNECTION);
        Connection connection = bindConnection.get();
        if (connection != null) {
            connection.close();
        }
    }
}
