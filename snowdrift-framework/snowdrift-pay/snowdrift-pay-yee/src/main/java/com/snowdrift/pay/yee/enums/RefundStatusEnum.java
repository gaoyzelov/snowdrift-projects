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
public enum RefundStatusEnum {

    SUCCESS("SUCCESS", "退款成功"),
    PROCESSING("PROCESSING", "退款处理中"),
    FAILED("FAILED", "退款失败"),
    CANCEL("CANCEL", "退款关闭"),
    SUSPEND("SUSPEND", "退款中断");

    private static final Map<String, RefundStatusEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (RefundStatusEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static RefundStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}