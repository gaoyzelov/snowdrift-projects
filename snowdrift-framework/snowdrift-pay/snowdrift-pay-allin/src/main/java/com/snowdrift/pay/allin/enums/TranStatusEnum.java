package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * TrxStatusEnum
 * 仅限收银台API接口使用
 *
 * @author gaoye
 * @date 2025/05/22 13:25:51
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum TranStatusEnum {

    SUCCESS("00", "交易成功"),
    PENDING("01", "待处理"),
    PROCESSING("20", "交易处理中"),
    FAIL("99", "交易失败");

    private static final Map<String, TranStatusEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(TranStatusEnum.values().length);
        for (TranStatusEnum value : TranStatusEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static TranStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}