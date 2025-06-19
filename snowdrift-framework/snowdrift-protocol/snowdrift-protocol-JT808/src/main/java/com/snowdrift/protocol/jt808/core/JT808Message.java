package com.snowdrift.protocol.jt808.core;

import lombok.Data;

import java.io.Serializable;

/**
 * JT808Message
 *
 * @author gaoye
 * @date 2025/06/19 10:41:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
public class JT808Message implements Serializable {

    private JT808Header header;

    private JT808Body body;

    private String dataPacket;

    private Byte crc;
}