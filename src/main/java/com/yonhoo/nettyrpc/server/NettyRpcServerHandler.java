package com.yonhoo.nettyrpc.server;

import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.exception.RpcErrorCode;
import com.yonhoo.nettyrpc.exception.RpcException;
import com.yonhoo.nettyrpc.protocol.RpcMessage;
import com.yonhoo.nettyrpc.protocol.RpcRequest;
import com.yonhoo.nettyrpc.protocol.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    public static final int REQUEST_FAIL = -1;
    public static final String CHANNEL_INACTIVE = "channel inactive";
    private static ConcurrentHashMap<String, ServerServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RpcMessage requestRpcMessage = ((RpcMessage) msg);
        try {
            log.info("server receive msg: [{}] ", msg);

            checkMessageValid((RpcMessage) msg);

            RpcRequest rpcRequestData = (RpcRequest) requestRpcMessage.getData();

            Object result = doRequestInvoke(rpcRequestData);

            RpcResponse responseMessage;
            if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                responseMessage = RpcResponse.success(result);
            } else {
                responseMessage = RpcResponse.fail(REQUEST_FAIL, CHANNEL_INACTIVE);
                log.error("not writable now, message dropped");
            }

            ctx.writeAndFlush(requestRpcMessage.SuccessResponse(responseMessage)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        } catch (Exception e) {
            log.error("handle request error", e);
            RpcResponse rpcResponse = RpcResponse.fail(REQUEST_FAIL, e.getMessage());
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcResponse)
                    .requestId(requestRpcMessage.getRequestId())
                    .codec(requestRpcMessage.getCodec())
                    .compress(requestRpcMessage.getCompress())
                    .messageType(RpcConstants.RESPONSE_TYPE)
                    .build();
            ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        } finally {
            //Ensure that ByteBuf is released, otherwise there may be memory leaks
            ReferenceCountUtil.release(msg);
        }
    }

    private Object doRequestInvoke(RpcRequest rpcRequest) {

        String serviceName = rpcRequest.getServiceName();
        ServerServiceDefinition serverServiceDefinition = serviceDefinitionMap.get(serviceName);

        if (serverServiceDefinition == null) {
            throw RpcException.with(RpcErrorCode.SERVICE_NOT_REGISTERED);
        }

        return serverServiceDefinition.invokeMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes(), rpcRequest.getParameters());
    }

    private void checkMessageValid(RpcMessage msg) {
        if (RpcConstants.PROTOCOL_DEFAULT_TYPE != msg.getCodec()) {
            throw RpcException.with(RpcErrorCode.CODEC_NOT_SUPPORTED_THIS_TYPE);
        }
        if (RpcConstants.REQUEST_TYPE != msg.getMessageType()) {
            throw RpcException.with(RpcErrorCode.CHANNEL_READ_IS_NOT_REQUEST_TYPE);
        }
    }

    public static void setServiceDefinition(List<ServerServiceDefinition> serviceDefinitionList) {
        serviceDefinitionList.forEach(serverServiceDefinition ->
                serviceDefinitionMap.put(serverServiceDefinition.getServiceName(), serverServiceDefinition)
        );
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
