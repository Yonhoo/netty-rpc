package com.yonhoo.nettyrpc.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RpcMessageDecoderUnitTest {

    @Test
    void should_decode_response_message_success_when_call_rpc_message_decode_given_response_data() throws Exception {
        //given
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);

        ByteBuf out = Unpooled.buffer(50);
        RpcResponse response = new RpcResponse();
        response.setMessage("success");
        response.setCode(0);
        response.setData("hello world");
        RpcMessage message = RpcMessage.builder()
                .codec(RpcConstants.PROTOCOL_DEFAULT_TYPE)
                .messageType(RpcConstants.RESPONSE_TYPE)
                .compress(CompressTypeEnum.NONE.getCode())
                .requestId(1)
                .data(response)
                .build();

        RpcMessageEncoder rpcMessageEncoder = new RpcMessageEncoder();
        rpcMessageEncoder.encode(ctx, message, out);

        //when
        RpcMessageDecoder decoder = new RpcMessageDecoder();
        Method method = decoder.getClass().getDeclaredMethod("decodeFrame", ByteBuf.class);
        method.setAccessible(true);

        RpcMessage rpcMessage = (RpcMessage) method.invoke(decoder, out);

        //then
        assertThat(rpcMessage.getMessageType()).isEqualTo(RpcConstants.RESPONSE_TYPE);
        assertThat(rpcMessage.getCodec()).isEqualTo(RpcConstants.PROTOCOL_DEFAULT_TYPE);
        assertThat(rpcMessage.getRequestId()).isEqualTo(1);
        RpcResponse rpcResponse = (RpcResponse) rpcMessage.getData();
        assertThat(rpcResponse.getData()).isEqualTo("hello world");
        assertThat(rpcResponse.getCode()).isEqualTo(0);
        assertThat(rpcResponse.getMessage()).isEqualTo("success");

    }
}