package com.yonhoo.nettyrpc.protocol;


import static org.assertj.core.api.Assertions.assertThat;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.serialize.ProtostuffSerializer;
import com.yonhoo.nettyrpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProtocolNegotiatorUnitTest {

    public static final int HEAD_LENGTH = 16;
    public static final int TOTAL_LENGTH = 0;
    public static final int MAGIC_NUMBER_0 = 4;
    public static final int MAGIC_NUMBER_1 = 5;
    public static final int MAGIC_NUMBER_2 = 6;
    public static final int MAGIC_NUMBER_3 = 7;
    public static final int PROTOCOL_VERSION = 8;
    public static final int RESPONSE_TYPE = 9;
    public static final int CODEC_TYPE = 10;
    public static final int COMPRESS_TYPE = 11;
    public static final int REQUEST_ID = 12;
    public static final int BODY = 16;

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

        assertThat(out.getInt(TOTAL_LENGTH)).isEqualTo(totalLength);
        assertThat(out.getByte(MAGIC_NUMBER_0)).isEqualTo(RpcConstants.MAGIC_NUMBER[0]);
        assertThat(out.getByte(MAGIC_NUMBER_1)).isEqualTo(RpcConstants.MAGIC_NUMBER[1]);
        assertThat(out.getByte(MAGIC_NUMBER_2)).isEqualTo(RpcConstants.MAGIC_NUMBER[2]);
        assertThat(out.getByte(MAGIC_NUMBER_3)).isEqualTo(RpcConstants.MAGIC_NUMBER[3]);
        assertThat(out.getByte(PROTOCOL_VERSION)).isEqualTo(RpcConstants.VERSION);
        assertThat(out.getByte(RESPONSE_TYPE)).isEqualTo(RpcConstants.RESPONSE_TYPE);
        assertThat(out.getByte(CODEC_TYPE)).isEqualTo((byte) 0);
        assertThat(out.getByte(COMPRESS_TYPE)).isEqualTo(CompressTypeEnum.NONE.getCode());
        assertThat(out.getInt(REQUEST_ID)).isEqualTo(1);
        byte[] encodeData = new byte[serializeData.length];
        out.getBytes(BODY, encodeData);
        assertThat(encodeData).isEqualTo(serializeData);

    }

    @Test
    void should_encode_error_type_response_protocol_when_encode_error() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);

        ByteBuf out = Unpooled.buffer(50);
        RpcMessage message = RpcMessage.builder().build();

        ProtocolNegotiator protocolNegotiator = new ProtocolNegotiator();
        protocolNegotiator.encode(ctx, message, out);

        assertThat(out.getInt(TOTAL_LENGTH)).isEqualTo(46);
        assertThat(out.getByte(MAGIC_NUMBER_0)).isEqualTo(RpcConstants.MAGIC_NUMBER[0]);
        assertThat(out.getByte(MAGIC_NUMBER_1)).isEqualTo(RpcConstants.MAGIC_NUMBER[1]);
        assertThat(out.getByte(MAGIC_NUMBER_2)).isEqualTo(RpcConstants.MAGIC_NUMBER[2]);
        assertThat(out.getByte(MAGIC_NUMBER_3)).isEqualTo(RpcConstants.MAGIC_NUMBER[3]);
        assertThat(out.getByte(PROTOCOL_VERSION)).isEqualTo(RpcConstants.VERSION);
        assertThat(out.getByte(RESPONSE_TYPE)).isEqualTo(RpcConstants.ERROR_TYPE);
        assertThat(out.getByte(CODEC_TYPE)).isEqualTo(RpcConstants.PROTOCOL_DEFAULT_TYPE);
        assertThat(out.getByte(COMPRESS_TYPE)).isEqualTo(CompressTypeEnum.NONE.getCode());
        assertThat(out.getInt(REQUEST_ID)).isEqualTo(1);

    }
}