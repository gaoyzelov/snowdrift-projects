package com.snowdrift.protocol.jt808.consts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * JT808Const
 *
 * @author gaoye
 * @date 2025/06/19 11:39:50
 * @description xxxxxxxx
 * @since 1.0
 */
public class JT808Const {

    private static final byte MARK = (byte) 0x7E;

    public static final ByteBuf DELIMITER = Unpooled.wrappedBuffer(new byte[]{MARK});
}