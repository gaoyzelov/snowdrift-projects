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
public enum PromotionTypeEnum {

    CUSTOM_REDUCTION("CUSTOM_REDUCTION", "自定义金额立减"),
    CUSTOM_ALLOWANCE("CUSTOM_ALLOWANCE", "自定义金额补贴"),
    CASH_COUPON("CASH_COUPON", "代金券"),
    FULL_DISCOUNT_COUPON("FULL_DISCOUNT_COUPON", "满减券");

    private static final Map<String, PromotionTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (PromotionTypeEnum value : PromotionTypeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public PromotionTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}