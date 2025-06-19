package com.snowdrift.protocol.jt808.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.snowdrift.protocol.jt808.core.JT808Message;
import com.snowdrift.protocol.jt808.core.JT808MessageType;
import com.snowdrift.protocol.jt808.util.ByteUtil;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.Charset;

/**
 * MessageInputHandler
 *
 * @author gaoye
 * @date 2025/06/19 10:07:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public class JT808MessageInputHandler extends SimpleChannelInboundHandler<JT808Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JT808Message msg) throws Exception {
        log.info("接收到消息：{}", JSON.toJSONString(msg, JSONWriter.Feature.PrettyFormat));
        if (msg.getHeader().getMsgType() == JT808MessageType.TERMINAL_REGISTER) {
            byte[] msgIds = ByteUtil.shortToBytes((short) 0x8100);
            byte res = 0;
            byte[] authCode = "hello!".getBytes(Charset.forName("GBK"));
            byte[] bytes = ArrayUtils.addAll(msgIds, res);
            bytes = ArrayUtils.addAll(bytes, authCode);
            bytes = ArrayUtils.addAll(bytes, ByteUtil.calcCrc(bytes));
            bytes = ByteUtil.transform(bytes);
            byte[] response = new byte[bytes.length + 2];
            response[0] = (byte) 0x7e;
            System.arraycopy(bytes, 0, response, 1, bytes.length);
            response[response.length - 1] = (byte) 0x7e;
            log.info("响应消息：{}", ByteBufUtil.hexDump(response));
            ctx.writeAndFlush(Unpooled.copiedBuffer(response));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}