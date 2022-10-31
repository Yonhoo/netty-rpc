package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {
    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> streamIdPromiseMap =
            new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        try {
            CompletableFuture<RpcResponse> responseFuture = streamIdPromiseMap.remove(rpcMessage.getRequestId());
            if (RpcConstants.RESPONSE_TYPE == rpcMessage.getMessageType()) {
                responseFuture.complete((RpcResponse) rpcMessage.getData());
            }
        } catch (NullPointerException e) {
            log.warn("this stream message was removed: {}", rpcMessage);
        }

    }

    public void setStreamResponsePromise(Integer streamId, CompletableFuture<RpcResponse> responsePromise) {
        streamIdPromiseMap.put(streamId, responsePromise);
    }

    public CompletableFuture<RpcResponse> getStreamResponsePromise(Integer streamId) {
        return streamIdPromiseMap.get(streamId);
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
        streamIdPromiseMap.forEach((streamId, responsePromise) -> {
            if (!responsePromise.isDone()) {
                responsePromise.complete(RpcResponse.fail(-1, "channel close"));
            }
        });
        streamIdPromiseMap.clear();
    }
}
