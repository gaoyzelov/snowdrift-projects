package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * TrxStatusEnum
 * 仅限收银台接口使用
 *
 * @author gaoye
 * @date 2025/05/22 13:25:51
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum TrxCodeEnum {

    WX_PAY("VSP501", "微信支付"),
    WX_CANCEL("VSP502", "微信支付撤销"),
    WX_REFUND("VSP503", "微信支付退款"),
    ALI_PAY("VSP511", "支付宝支付"),
    ALI_CANCEL("VSP512", "支付宝支付撤销"),
    ALI_REFUND("VSP513", "支付宝支付退款"),
    SCAN_PAY("VSP541", "扫码支付"),
    SCAN_CANCEL("VSP542", "扫码撤销"),
    SCAN_REFUND("VSP543", "扫码支付退货"),
    UNION_SCAN_PAY("VSP551", "银联扫码支付"),
    UNION_SCAN_CANCEL("VSP552", "银联扫码撤销"),
    UNION_SCAN_REFUND("VSP553", "银联扫码退货"),
    ERROR_DEBIT_ADJUSTMENT("VSP907", "差错借记调整"),
    ERROR_CREDIT_ADJUSTMENT("VSP908", "差错贷记调整"),
    DIGITAL_PAY("VSP611", "数字货币支付"),
    DIGITAL_CANCEL("VSP612", "数字货币撤销"),
    DIGITAL_REFUND("VSP613", "数字货币退货"),
    INSTALLMENT_PAY("VSP621", "分期支付"),
    INSTALLMENT_CANCEL("VSP622", "分期撤销"),
    INSTALLMENT_REFUND("VSP623", "分期退货"),
    RECHARGE("300002", "充值"),
    WX_ORDER_PAY("VSP681", "微信订单预消费"),
    WX_ORDER_REFUND("VSP682", "微信订单退款"),
    WX_ORDER_PAY_COMPLETE("VSP683", "微信订单完成"),
    WX_ORDER_REFUND_COMPLETE("VSP684", "微信订单退款完成");

    private static final Map<String, TrxCodeEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(TrxCodeEnum.values().length);
        for (TrxCodeEnum value : TrxCodeEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static TrxCodeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}