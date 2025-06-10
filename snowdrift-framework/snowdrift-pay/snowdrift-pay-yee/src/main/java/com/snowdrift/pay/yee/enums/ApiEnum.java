package com.snowdrift.pay.yee.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ApiEnum
 *
 * @author gaoye
 * @date 2025/06/05 18:50:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ApiEnum {

    AGGREGATE_CODE("/rest/v1.0/aggpay/pay-link", "POST", "生成聚合订单码"),
    UNI_ORDER("/rest/v1.0/aggpay/pre-pay", "POST", "聚合支付统一下单"),
    ORDER_QUERY("/rest/v1.0/trade/order/query", "GET", "订单查询"),
    REFUND_APPLY("/rest/v1.0/trade/refund", "POST", "申请退款"),
    REFUND_QUERY("/rest/v1.0/trade/refund/query", "GET", "查询退款"),
    WITHDRAW_APPLY("/rest/v1.0/account/withdraw/order", "POST", "提现申请"),
    WITHDRAW_QUERY("/rest/v1.0/account/withdraw/system/query", "GET", "提现查询"),
    WITHDRAW_CARD_BIND("/rest/v1.0/account/withdraw/card/bind","POST","提现卡-添加"),
    WITHDRAW_CARD_QUERY("/rest/v1.0/account/withdraw/card/query","GET","提现卡-查询"),
    WITHDRAW_CARD_MODIFY("/rest/v1.0/account/withdraw/card/modify","POST","提现卡-修改/注销");

    private final String uri;
    private final String method;
    private final String note;
}