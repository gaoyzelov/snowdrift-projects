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
public enum BankCardTypeEnum {

    DEBIT_CARD("DEBIT_CARD", "借记卡"),
    ENTERPRISE_ACCOUNT("ENTERPRISE_ACCOUNT", "对公账号"),
    UNIT_SETTLEMENT_CARD("UNIT_SETTLEMENT_CARD", "单位结算卡");

    private static final Map<String, BankCardTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (BankCardTypeEnum value : BankCardTypeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public BankCardTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}