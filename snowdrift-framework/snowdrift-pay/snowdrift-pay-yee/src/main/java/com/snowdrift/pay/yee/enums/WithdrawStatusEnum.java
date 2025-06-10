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
public enum WithdrawStatusEnum {

    REQUEST_RECEIVE("REQUEST_RECEIVE", "请求已接收"),
    REQUEST_ACCEPT("REQUEST_ACCEPT", "请求已受理"),
    SUCCESS("SUCCESS", "已到账"),
    FAIL("FAIL", "失败"),
    REMITING("REMITING", "银行处理中");

    private static final Map<String, WithdrawStatusEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (WithdrawStatusEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static WithdrawStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}