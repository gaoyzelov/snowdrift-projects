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
public enum OrderStatusEnum {

    PROCESSING("PROCESSING", "订单待支付"),
    SUCCESS("SUCCESS", "订单支付成功"),
    TIME_OUT("TIME_OUT", "订单已过期"),
    FAIL("FAIL", "订单支付失败"),
    CLOSE("CLOSE", "订单关闭");

    private static final Map<String, OrderStatusEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (OrderStatusEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static OrderStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}