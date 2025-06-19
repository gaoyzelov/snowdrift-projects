package com.snowdrift.protocol.jt808.core;

import lombok.Data;

import java.io.Serializable;

/**
 * JT808Header
 *
 * @author gaoye
 * @date 2025/06/19 11:49:22
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
public class JT808Header implements Serializable {

    private JT808MessageType msgType;

    private Attribute attribute;

    private byte version;

    private String terminalNo;

    private short serialNo;

    private short pkgCount;

    private short pkgIndex;

    @Data
    public static class Attribute {

        private int msgLength;

        private boolean encrypt;

        private boolean split;

        private boolean hasVersion;
    }
}