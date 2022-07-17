package com.yonhoo.nettyrpc.protocol;


import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.serialize.ProtostuffSerializer;
import com.yonhoo.nettyrpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProtocolNegotiatorUnitTest {

    public static final int HEAD_LENGTH = 16;

    @Test
    void should_encode_response_protocol_given_response_msg() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);

        ByteBuf out = Unpooled.buffer(50);
        String data = "hello world";
        RpcMessage message = RpcMessage.builder()
                .codec((byte) 0)
                .messageType(RpcConstants.RESPONSE_TYPE)
                .compress(CompressTypeEnum.NONE.getCode())
                .requestId(1)
                .data(data)
                .build();
        Serializer serializer = new ProtostuffSerializer();
        byte[] serializeData = serializer.serialize(data);
        int totalLength = HEAD_LENGTH + serializeData.length;
        ProtocolNegotiator protocolNegotiator = new ProtocolNegotiator();
        protocolNegotiator.encode(ctx, message, out);
        int length = out.getInt(0);

        assertThat(length).isEqualTo(totalLength);
        assertThat(out.getByte(4)).isEqualTo(RpcConstants.MAGIC_NUMBER[0]);
        assertThat(out.getByte(4 + 1)).isEqualTo(RpcConstants.MAGIC_NUMBER[1]);
        assertThat(out.getByte(4 + 2)).isEqualTo(RpcConstants.MAGIC_NUMBER[2]);
        assertThat(out.getByte(4 + 3)).isEqualTo(RpcConstants.MAGIC_NUMBER[3]);
        assertThat(out.getByte(8)).isEqualTo(RpcConstants.VERSION);
        assertThat(out.getByte(9)).isEqualTo(RpcConstants.RESPONSE_TYPE);
        assertThat(out.getByte(10)).isEqualTo((byte) 0);
        assertThat(out.getByte(11)).isEqualTo(CompressTypeEnum.NONE.getCode());
        assertThat(out.getInt(12)).isEqualTo(1);
        byte[] encodeData = new byte[serializeData.length];
        out.getBytes(16, encodeData);
        assertThat(encodeData).isEqualTo(serializeData);

    }
}