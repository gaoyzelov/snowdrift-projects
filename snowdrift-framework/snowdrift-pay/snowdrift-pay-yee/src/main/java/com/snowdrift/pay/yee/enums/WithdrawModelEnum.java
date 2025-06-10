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
public enum WithdrawModelEnum {

    INNER_ACCOUNT_WITHDRAW("INNER_ACCOUNT_WITHDRAW", "易宝内部账户模式"),
    BANK_ACCOUNT_WITHDRAW("BANK_ACCOUNT_WITHDRAW", "银行清分银行账户模式");

    private static final Map<String, WithdrawModelEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (WithdrawModelEnum value : WithdrawModelEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public WithdrawModelEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}