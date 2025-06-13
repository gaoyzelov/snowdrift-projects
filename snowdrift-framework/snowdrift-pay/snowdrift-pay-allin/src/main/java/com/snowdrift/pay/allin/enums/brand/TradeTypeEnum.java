package com.snowdrift.pay.allin.enums.brand;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * MemberRoleEnum
 *
 * @author gaoye
 * @date 2025/06/03 16:32:34
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum TradeTypeEnum {

    OFFLINE_STORE("00", "线下门店购物"),
    ONLINE_MALL("01", "线上商城购物");

    private static final Map<String, TradeTypeEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(TradeTypeEnum.values().length);
        for (TradeTypeEnum value : TradeTypeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static TradeTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}