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
public enum PayWayEnum {

    USER_SCAN("USER_SCAN", "用户扫码"),
    MERCHANT_SCAN("MERCHANT_SCAN", "商家扫码"),
    JS_PAY("JS_PAY", "JS支付"),
    MINI_PROGRAM("MINI_PROGRAM", "小程序支付"),
    WECHAT_OFFIACCOUNT("WECHAT_OFFIACCOUNT", "微信公众号支付"),
    ALIPAY_LIFE("ALIPAY_LIFE", "生活号支付"),
    FACE_SCAN_PAY("FACE_SCAN_PAY", "刷脸支付"),
    SDK_PAY("SDK_PAY", "SDK支付"),
    H5_PAY("H5_PAY", "H5支付"),
    ONEKEYPAY("ONEKEYPAY", "一键支付"),
    BINDCARDPAY("BINDCARDPAY", "绑卡支付"),
    E_BANK("E_BANK", "网银支付"),
    ENTERPRISE_ACCOUNT_PAY("ENTERPRISE_ACCOUNT_PAY", "企业账户支付"),
    STATICQR ("STATICQR ", "静态台牌"),
    BANKTRANSFERPAY("BANKTRANSFERPAY", "银行转账支付"),
    DIRECT_PAY("DIRECT_PAY", "云微直接支付");

    private static final Map<String,PayWayEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (PayWayEnum value : PayWayEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static PayWayEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}