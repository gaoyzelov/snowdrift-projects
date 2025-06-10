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
public enum BankCardOptTypeEnum {

    MODIFY("MODIFY", "修改"),
    CANCELLED("CANCELLED", "注销");

    private static final Map<String, BankCardOptTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (BankCardOptTypeEnum value : BankCardOptTypeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public BankCardOptTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}