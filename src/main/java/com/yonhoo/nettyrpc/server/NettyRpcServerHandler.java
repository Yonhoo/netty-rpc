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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    public static final int REQUEST_FAIL = -1;
    public static final String CHANNEL_INACTIVE = "channel inactive";
    private Map<String, ServerServiceDefinition> serviceDefinitionMap;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);

                RpcMessage.RpcMessageBuilder rpcMessage = RpcMessage.builder();

                checkMessageValid((RpcMessage) msg);

                RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();

                Object result = doRequestInvoke(rpcRequest);

                rpcMessage.messageType(RpcConstants.RESPONSE_TYPE);
                if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                    RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                    rpcMessage.data(rpcResponse);
                } else {
                    RpcResponse<Object> rpcResponse = RpcResponse.fail(REQUEST_FAIL, CHANNEL_INACTIVE);
                    rpcMessage.data(rpcResponse);
                    log.error("not writable now, message dropped");
                }

                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } catch (Exception e) {
            log.error("handle request error", e);
            RpcResponse<Object> rpcResponse = RpcResponse.fail(REQUEST_FAIL, e.getMessage());
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcResponse)
                    .messageType(RpcConstants.ERROR_TYPE)
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

        return serverServiceDefinition.invokeMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
    }

    private void checkMessageValid(RpcMessage msg) {
        if (RpcConstants.PROTOCOL_DEFAULT_TYPE != msg.getCodec()) {
            throw RpcException.with(RpcErrorCode.CODEC_NOT_SUPPORTED_THIS_TYPE);
        }
        if (RpcConstants.REQUEST_TYPE != msg.getMessageType()) {
            throw RpcException.with(RpcErrorCode.CHANNEL_READ_IS_NOT_REQUEST_TYPE);
        }
    }

    public void setServiceDefiniton(Map<String, ServerServiceDefinition> serviceDefinitionMap) {
        this.serviceDefinitionMap = serviceDefinitionMap;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
