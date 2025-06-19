package com.snowdrift.protocol.jt808.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * JT808MessageType
 *
 * @author gaoye
 * @date 2025/06/19 10:43:01
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum JT808MessageType {

    TERMINAL_RESPONSE(0x0001, "终端通用应答"),
    TERMINAL_HEARTBEAT(0x0002, "终端心跳"),
    PLATFORM_RESPONSE(0x8001, "平台通用应答"),
    TERMINAL_REGISTER(0x0100, "终端注册");

    private static final Map<Integer, JT808MessageType> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (JT808MessageType type : values()){
            CODE_MAP.put(type.getCode(), type);
        }
    }
    private final Integer code;
    private final String note;

    public static JT808MessageType getByCode(Integer code){
        return CODE_MAP.get(code);
    }
}