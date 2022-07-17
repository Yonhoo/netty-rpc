package com.yonhoo.nettyrpc.protocol;

import com.yonhoo.nettyrpc.common.CompressTypeEnum;
import com.yonhoo.nettyrpc.common.RpcConstants;
import com.yonhoo.nettyrpc.serialize.ProtostuffSerializer;
import com.yonhoo.nettyrpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * custom protocol decoder
 * <p>
 * <pre>
 *   0     1     2     3     4    5    6    7    8         9            10     11      12    13    14   15  16
 *   +-----+-----+-----+-----+----+----+----+----+---------+------------+------+--------+----+----+-----+----+
 *   |   full length        | magic   code       | version | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 **/

@Slf4j
public class ProtocolNegotiator extends MessageToByteEncoder<RpcMessage> {

    private static AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) {
        int tailIndex = out.writerIndex();
        try {
            writeHead(rpcMessage, out);

            int fullLength = writeBody(rpcMessage.getData(), out);

            fillFullLength(out, fullLength);
        } catch (Exception e) {
            out.writerIndex(tailIndex);
            log.error("ProtocolNegotiator encode error ", e);
            writeErrorHead(out);
            int fullLength = writeBody(e.getMessage(), out);

            fillFullLength(out, fullLength);
        }
    }

    private int writeBody(Object data, ByteBuf out) {
        int fullLength = RpcConstants.HEAD_LENGTH;

        if (data == null) {
            return fullLength;
        }

        Serializer serializer = new ProtostuffSerializer();
        log.info("default serialize is protobuffer");
        byte[] bodyBytes = serializer.serialize(data);
        fullLength += bodyBytes.length;

        out.writeBytes(bodyBytes);

        return fullLength;
    }

    private void writeErrorHead(ByteBuf out) {
        //leave a place to write the value of full length
        out.writerIndex(out.writerIndex() + 4);

        out.writeBytes(RpcConstants.MAGIC_NUMBER);
        out.writeByte(RpcConstants.VERSION);

        out.writeByte(RpcConstants.ERROR_TYPE);
        out.writeByte(RpcConstants.PROTOCOL_DEFAULT_TYPE);
        out.writeByte(CompressTypeEnum.NONE.getCode());

        // reuqest id
        out.writeInt(ATOMIC_INTEGER.incrementAndGet());
    }

    private void writeHead(RpcMessage rpcMessage, ByteBuf out) {
        //leave a place to write the value of full length
        out.writerIndex(out.writerIndex() + 4);

        out.writeBytes(RpcConstants.MAGIC_NUMBER);
        out.writeByte(RpcConstants.VERSION);

        if (rpcMessage.getMessageType() == 0) {
            throw new RuntimeException("message type not be null");
        }
        out.writeByte(rpcMessage.getMessageType());
        out.writeByte(rpcMessage.getCodec());
        if (CompressTypeEnum.GZIP.getCode() == rpcMessage.getCompress()) {
            out.writeByte(CompressTypeEnum.GZIP.getCode());
        } else {
            out.writeByte(CompressTypeEnum.NONE.getCode());
        }

        // reuqest id
        out.writeInt(ATOMIC_INTEGER.incrementAndGet());
    }

    private void fillFullLength(ByteBuf out, int fullLength) {
        int writeIndex = out.writerIndex();
        out.writerIndex(writeIndex - fullLength);
        out.writeInt(fullLength);
        out.writerIndex(writeIndex);
    }
}
