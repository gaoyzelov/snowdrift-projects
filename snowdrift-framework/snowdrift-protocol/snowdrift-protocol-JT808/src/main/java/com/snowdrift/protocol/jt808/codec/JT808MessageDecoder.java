package com.snowdrift.protocol.jt808.codec;

import com.snowdrift.protocol.jt808.body.JT808TerminalRegisterBody;
import com.snowdrift.protocol.jt808.core.JT808Body;
import com.snowdrift.protocol.jt808.core.JT808Header;
import com.snowdrift.protocol.jt808.core.JT808Message;
import com.snowdrift.protocol.jt808.core.JT808MessageType;
import com.snowdrift.protocol.jt808.util.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.List;

/**
 * JT808MessageDecoder
 *
 * @author gaoye
 * @date 2025/06/19 10:41:43
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public class JT808MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.readableBytes() < 17) {
            log.error("数据长度有误");
            byteBuf.release();
            return;
        }
        // 构建消息
        JT808Message jt808Message = new JT808Message();
        // 获取数据包
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        // 原始报文
        String dataPacket = String.format("7E%s7E", ByteBufUtil.hexDump(bytes));
        bytes = ByteUtil.restore(bytes);
        if (bytes == null){
            log.error("数据包有误");
            return;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        //解析请求头
        JT808Header jt808Header = decodeHeader(buf);
        if (jt808Header.getAttribute().isSplit()) {
            // 消息分包
            jt808Header.setPkgCount(buf.readShort());
            jt808Header.setPkgIndex(buf.readShort());
        }
        JT808Body jt808Body = decodeBody(jt808Header, buf);
        jt808Message.setHeader(jt808Header);
        jt808Message.setBody(jt808Body);
        jt808Message.setDataPacket(dataPacket);
        out.add(jt808Message);
    }

    private JT808Header decodeHeader(ByteBuf byteBuf) {
        JT808Header jt808Header = new JT808Header();
        // 消息ID 2字节
        jt808Header.setMsgType(JT808MessageType.getByCode((int) byteBuf.readShort()));
        // 消息体属性 2字节
        short attr = byteBuf.readShort();
        JT808Header.Attribute attribute = new JT808Header.Attribute();
        attribute.setMsgLength(attr & 0x03FF);
        attribute.setEncrypt((attr & 0x0400) >> 10 == 1);
        attribute.setSplit((attr & 0x2000) >> 13 == 1);
        attribute.setHasVersion((attr & 0x4000) >> 14 == 1);
        jt808Header.setAttribute(attribute);
        // 协议版本号 1字节
        jt808Header.setVersion(byteBuf.readByte());
        // 终端手机号 10字节
        byte[] bytes = new byte[10];
        byteBuf.readBytes(bytes);
        jt808Header.setTerminalNo(StringUtils.stripStart(ByteUtil.bytesToBCD(bytes), "0"));
        // 消息流水号 2字节
        jt808Header.setSerialNo(byteBuf.readShort());
        return jt808Header;
    }

    private JT808Body decodeBody(JT808Header jt808Header, ByteBuf byteBuf) {
        JT808TerminalRegisterBody jt808TerminalRegisterBody = new JT808TerminalRegisterBody();
        jt808TerminalRegisterBody.setProvince(byteBuf.readShort());
        jt808TerminalRegisterBody.setCity(byteBuf.readShort());
        byte[] bytes = new byte[11];
        byteBuf.readBytes(bytes);
        jt808TerminalRegisterBody.setManufacturerId(new String(bytes).trim());
        bytes = new byte[30];
        byteBuf.readBytes(bytes);
        jt808TerminalRegisterBody.setTerminalType(new String(bytes).trim());
        bytes = new byte[30];
        byteBuf.readBytes(bytes);
        jt808TerminalRegisterBody.setTerminalId(new String(bytes).trim());
        jt808TerminalRegisterBody.setColor(byteBuf.readByte());
        bytes = new byte[jt808Header.getAttribute().getMsgLength() - 76];
        byteBuf.readBytes(bytes);
        jt808TerminalRegisterBody.setPlateNo(new String(bytes, Charset.forName("GBK")).trim());
        return jt808TerminalRegisterBody;
    }
}