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
public enum PayMethodEnum {

    SCAN_ALI("SCAN_ALIPAY", "支付宝（主扫），C端用支付宝扫一扫B端收款码"),
    SCAN_WX("SCAN_WEIXIN", "微信（主扫），C端用微信扫一扫B端收款码"),
    CODE_PAY("CODEPAY_VSP", "微信&支付宝&银联被扫，C端出示付款码，B端通过扫描枪等工具扫码"),
    WX_MP("WECHAT_PUBLIC", "微信公众号支付"),
    H5("H5_CASHIER_VSP", "H5收银台支付，适用于微信、支付宝自带浏览器"),
    WX_MA("WECHATPAY_MINIPROGRAM", "微信小程序支付"),
    GATEWAY("GATEWAY_VSP", "网关支付，适用于PC端"),
    POS_ORDER("ORDER_VSPPAY", "POS订单支付"),
    BALANCE_PROTOCOL("BALANCE_PROTOCOL", "账户余额协议支付"),
    ALI_JS("ALIPAY_SERVICE", "支付宝JS支付"),
    ALI_MA_CASHIER("ALIPAY_MINIPROGRAM_CASHIER_VSP", "支付宝小程序收银台支付"),
    WX_MA_CASHIER("WECHAT_MINIPROGRAM_CASHIER_VSP", "微信小程序收银台支付"),
    SYB("QUICKPAY_VSP", "收银宝快捷支付"),
    PHONE("MOBILEAPPPAY_VSP", "手机控件支付"),
    SYN_CLOUD("YWPAY_VSP", "收银宝云微支付");

    private static final Map<String, PayMethodEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(PayMethodEnum.values().length);
        for (PayMethodEnum value : PayMethodEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static PayMethodEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}