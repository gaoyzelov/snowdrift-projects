package com.snowdrift.protocol.jt808.codec;

import com.snowdrift.protocol.jt808.core.JT808Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * JT808Encoder
 *
 * @author gaoye
 * @date 2025/06/19 10:40:18
 * @description xxxxxxxx
 * @since 1.0
 */
public class JT808MessageEncoder extends MessageToByteEncoder<JT808Message> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, JT808Message jt808Message, ByteBuf byteBuf) throws Exception {

    }
}