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
public enum ClearStatusEnum {

    SUCCESS("SUCCESS", "成功"),
    PROCESSING("PROCESSING", "处理中");

    private static final Map<String, ClearStatusEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (ClearStatusEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static ClearStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}