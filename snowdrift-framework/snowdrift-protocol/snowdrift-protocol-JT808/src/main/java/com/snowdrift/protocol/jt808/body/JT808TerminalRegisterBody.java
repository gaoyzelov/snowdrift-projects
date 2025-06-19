package com.snowdrift.protocol.jt808.body;

import com.snowdrift.protocol.jt808.core.JT808Body;
import lombok.Data;

/**
 * JT808TerminalRegisterBody
 *
 * @author gaoye
 * @date 2025/06/19 16:49:34
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
public class JT808TerminalRegisterBody implements JT808Body {

    private short province;

    private short city;

    private String manufacturerId;

    private String terminalType;

    private String terminalId;

    private byte color;

    private String plateNo;
}