package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {
    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> streamIdPromiseMap =
            new ConcurrentHashMap<>();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMessage rpcMessage = (RpcMessage) msg;
        CompletableFuture<RpcResponse> responseFuture = streamIdPromiseMap.get(rpcMessage.getRequestId());
        if (RpcConstants.RESPONSE_TYPE == rpcMessage.getMessageType()) {
            responseFuture.complete((RpcResponse) rpcMessage.getData());
        } else if (RpcConstants.ERROR_TYPE == rpcMessage.getMessageType()) {
            throw new RuntimeException(((RpcResponse) rpcMessage.getData()).getMessage());
        }

    }

    public void setStreamResponsePromise(Integer streamId, CompletableFuture<RpcResponse> responsePromise) {
        streamIdPromiseMap.put(streamId, responsePromise);
    }

    public CompletableFuture<RpcResponse> getStreamResponsePromise(Integer streamId) {
        return streamIdPromiseMap.get(streamId);
    }

    public Object onException(Throwable e) {
        return null;
    }
}
