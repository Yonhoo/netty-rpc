package com.yonhoo.nettyrpc.protocol;

import com.yonhoo.nettyrpc.common.RpcConstants;
import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

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


@Value
@Builder
public class RpcMessage implements Serializable {
    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id
     */
    private int requestId;
    /**
     * request data
     */
    private Object data;

    public RpcMessage SuccessResponse(Object data) {
        return RpcMessage.builder()
                .requestId(requestId)
                .messageType(RpcConstants.RESPONSE_TYPE)
                .codec(codec)
                .compress(compress)
                .data(data)
                .build();
    }
}
