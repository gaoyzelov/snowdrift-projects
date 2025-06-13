package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * PayTypeEnum
 *
 * @author gaoye
 * @date 2025/05/22 16:17:14
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum PayTypeEnum {
    WX_SCAN("W01", "微信扫码支付"),
    WX_JS("W02", "微信JS支付"),
    WX_APP("W03", "微信APP支付"),
    WX_MINI_PROGRAM("W06", "微信小程序支付"),
    WX_ORDER("W11", "微信订单支付"),
    ALI_SCAN("A01", "支付宝扫码支付"),
    ALI_JS("A02", "支付宝JS支付"),
    ALI_APP("A03", "支付宝APP支付"),
    UNION_SCAN("U01", "银联扫码支付"),
    UNION_JS("U02", "银联JS支付"),
    DIGITAL_SCAN("S01", "数币扫码支付"),
    DIGITAL_APP_H5("S03", "数字货币H5/APP"),
    NET_BANK("N03", "网联支付");

    private static final Map<String, PayTypeEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>(PayTypeEnum.values().length);
        for (PayTypeEnum payTypeEnum : PayTypeEnum.values()) {
            CODE_MAP.put(payTypeEnum.getCode(), payTypeEnum);
        }
    }

    private final String code;
    private final String note;

    public static PayTypeEnum getByCode(String code){
        return CODE_MAP.get(code);
    }
}