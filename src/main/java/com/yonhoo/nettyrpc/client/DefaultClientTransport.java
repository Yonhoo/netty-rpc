package com.yonhoo.nettyrpc.client;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.connection.Connection;
import com.yonhoo.nettyrpc.connection.DefaultClientConnectionManager;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import com.yonhoo.nettyrpc.registry.ProviderInfo;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultClientTransport {
    // default singleton
    private static final DefaultClientConnectionManager connectionManager = new DefaultClientConnectionManager();
    private static final AtomicInteger streamId = new AtomicInteger();

    public Object doSend(RpcRequest request, InvokeContext invokeContext) {
        ProviderInfo providerInfo = invokeContext.getProviderInfo();
        Connection connection = connectionManager.getConnection(providerInfo.getUrl());
        if (connection.isFine()) {
            try {
                RpcMessage rpcMessage = RpcMessage.builder()
                        .messageType(RpcConstants.REQUEST_TYPE)
                        .requestId(streamId.getAndIncrement())
                        .codec(RpcConstants.PROTOCOL_DEFAULT_TYPE)
                        .compress(CompressTypeEnum.NONE.getCode())
                        .data(request)
                        .build();

                CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
                connection.addInvokeFuture(rpcMessage.getRequestId(), responseFuture);
                //nettyRpcClientHandler.setStreamResponsePromise(rpcMessage.getRequestId(), responseFuture);
                //TODO add future listener handle
                connection.getChannel().writeAndFlush(rpcMessage)
                        .addListener(new FutureListener<Void>() {
                            public void operationComplete(Future<Void> f) throws Exception {
                                if (f.isSuccess()) {
                                    log.info("channel write message success");
                                } else {
                                    log.error("write message error:", f.cause());
                                }

                            }
                        });
                return responseFuture.get().getData();
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
}
