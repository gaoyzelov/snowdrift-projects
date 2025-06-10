package com.snowdrift.pay.yee.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * CodeEnum
 *
 * @author gaoye
 * @date 2025/06/06 09:21:25
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ReceiveTypeEnum {

    REAL_TIME("REAL_TIME", "实时"),
    TWO_HOUR("TWO_HOUR", "两小时"),
    NEXT_DAY("NEXT_DAY", "次日到账");

    private static final Map<String, ReceiveTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (ReceiveTypeEnum value : ReceiveTypeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public ReceiveTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}