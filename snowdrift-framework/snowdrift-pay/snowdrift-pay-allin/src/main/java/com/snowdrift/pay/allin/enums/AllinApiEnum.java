package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AllinApiEnum
 *
 * @author gaoye
 * @date 2025/05/20 19:51:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum AllinApiEnum {
    AGGREGATE_PAY("https://syb.allinpay.com/apiweb/h5unionpay/unionorder", "订单收款聚合码支付"),
    PAY_STATUS("https://vsp.allinpay.com/apiweb/tranx/query", "交易状态查询"),
    REFUND("https://vsp.allinpay.com/apiweb/tranx/refund", "交易退款"),
    CANCEL("https://vsp.allinpay.com/apiweb/tranx/cancel","交易撤销"),
    CLOSE("https://vsp.allinpay.com/apiweb/tranx/close","订单关闭"),
    BALANCE("https://vsp.allinpay.com/apiweb/cusacct/querybalance","商户账户余额查询"),
    BALANCE_SETTLEMENT("https://vsp.allinpay.com/apiweb/cusacct/withdraw","商户账户余额结算"),
    BALANCE_SETTLEMENT_QUERY("https://vsp.allinpay.com/apiweb/tranx/query","商户账户余额结算查询"),
    SETTLEMENT_DOCUMENT("https://cus.allinpay.com/cusapi/trxfile/setttrx","获取结算单"),
    STATEMENT_OF_ACCOUNT("https://cus.allinpay.com/cusapi/trxfile/get","获取对账单"),
    SHARE("https://vsp.allinpay.com/apiweb/trxshare/share","交易分账"),
    SHARE_REVOKE("https://vsp.allinpay.com/apiweb/trxshare/revoke","交易分账回退"),
    SHARE_QUERY("https://vsp.allinpay.com/apiweb/trxshare/query","交易分账查询"),
    SHARE_REVOKE_QUERY("https://vsp.allinpay.com/apiweb/trxshare/revokequery","交易分账回退查询"),
    UNI_APY("https://vsp.allinpay.com/apiweb/unitorder/pay","统一支付"),
    UNI_REFUND("https://vsp.allinpay.com/apiweb/tranx/refund","统一退款"),
    UNI_PAY_STATUS("https://vsp.allinpay.com/apiweb/tranx/query","统一查询"),
    UNI_CANCEL("https://vsp.allinpay.com/apiweb/tranx/cancel","统一撤销"),
    UNI_CLOSE("https://vsp.allinpay.com/apiweb/tranx/close","统一关闭");

    private final String url;
    private final String note;
}