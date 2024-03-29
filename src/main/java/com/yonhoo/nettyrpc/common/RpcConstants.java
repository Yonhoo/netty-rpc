package com.yonhoo.nettyrpc.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RpcConstants {
    /**
     * Magic number. Verify RpcMessage
     */
    public static final byte[] MAGIC_NUMBER = {(byte) 'g', (byte) 'r', (byte) 'p', (byte) 'c'};
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    //version information
    public static final byte VERSION = 1;
    public static final byte TOTAL_LENGTH = 16;

    //message type
    public static final byte REQUEST_TYPE = 1;
    public static final byte RESPONSE_TYPE = 2;
    public static final byte ERROR_TYPE = 3;

    //codec type
    public static final byte PROTOCOL_DEFAULT_TYPE = 1;

    //request error
    public static final String REQUEST_ERROR = "-1";

    //ping
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;
    //pong
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;
    public static final int HEAD_LENGTH = 16;
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    public static final String PROVIDER_NAME = "PROVIDER_NAME";
    public static final String SERVICE_WEIGHT = "SERVICE_WEIGHT";
}
