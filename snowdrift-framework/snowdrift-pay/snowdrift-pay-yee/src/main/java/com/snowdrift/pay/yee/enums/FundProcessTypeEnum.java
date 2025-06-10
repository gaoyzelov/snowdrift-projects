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
public enum FundProcessTypeEnum {

    DELAY_SETTLE("PROCESSING", "分账"),
    REAL_TIME("SUCCESS", "不分账"),
    REAL_TIME_DIVIDE("TIME_OUT", "实时分账");

    private static final Map<String, FundProcessTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (FundProcessTypeEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static FundProcessTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}