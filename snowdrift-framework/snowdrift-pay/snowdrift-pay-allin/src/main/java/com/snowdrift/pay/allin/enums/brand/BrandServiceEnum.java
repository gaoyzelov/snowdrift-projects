package com.snowdrift.pay.allin.enums.brand;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BrandServiceEnum
 *
 * @author gaoye
 * @date 2025/05/29 14:04:40
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum BrandServiceEnum {
    MEMBER_REGISTER("brand.member.register", "收款方创建"),
    MEMBER_REAL_NAME("brand.member.setRealName","收款方实名认证"),
    MEMBER_SET_COMPANY_INFO("brand.member.setCompanyInfo", "收款方企业信息设置"),
    MEMBER_BIND_PHONE_APPLY("brand.member.bindPhoneApply", "收款方短信认证触发"),
    MEMBER_BIND_PHONE_CONFIRM("brand.member.bindPhoneConfirm", "收款方短信认证回填"),
    MEMBER_BIND_CARD_APPLY("brand.member.bindCardApply","收款方银行卡验证"),
    MEMBER_UNBIND_CARD("brand.member.unbindCard","收款方银行卡解绑"),
    MEMBER_SIGN_CONTRACT("brand.member.signContract", "收款方电子协议签约"),
    MEMBER_QUERY("brand.member.queryInfo", "收款方信息查询"),
    MEMBER_BIND_CARD_QUERY("brand.member.queryBankCard","收款方绑定银行卡查询"),
    MEMBER_BALANCE("brand.member.queryBalance", "收款方余额查询"),
    MERCHANT_BALANCE("brand.merchant.queryMerchantBalance", "平台账户余额查询"),
    QUERY_PAYER_ID("brand.pay.getPayerId", "获取付款方ID"),
    PAYER_ID_CONFIRM("brand.pay.confirmPayerId","确认验证付款方ID"),
    PAY_APPLY("brand.pay.apply", "支付申请"),
    PAY_REFUND("brand.pay.refund","退款申请"),
    TRX_QUERY("brand.pay.query","交易结果查询"),
    TRX_CONFIRM("brand.pay.payConfirm","确认交易结果"),
    WITHDRAW_APPLY("brand.pay.withdrawApply", "提现申请"),
    TRANSFER_APPLY("brand.pay.transfer", "转账汇款申请"),
    TRANSFER_QUERY("brand.pay.transferQuery", "转账汇款交易结果查询"),
    FREEZE_MONEY("brand.freeze.money", "冻结金额"),
    UNFREEZE_MONEY("brand.unfreeze.money", "解冻金额"),
    AGENT_COLLECT_APPLY("brand.pay.agentCollectApply","托管代收申请"),
    AGENT_PAY_APPLY("brand.pay.singleAgentPay","托管代付申请"),
    DOCUMENT_SYNC("brand.biz.syncDocument",  "单据同步"),
    DOCUMENT_TYPE_QUERY("brand.biz.queryDocumentType", "单据类型查询");


    private final String code;

    private final String note;
}