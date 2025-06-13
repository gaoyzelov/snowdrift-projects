package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * NotifyFieldEnum
 *
 * @author gaoye
 * @date 2025/05/22 13:54:56
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum NotifyFieldEnum {

    APPID("appid","收银宝APPID"),
    OUT_TRX_ID("outtrxid","第三方交易号"),
    TRX_CODE("trxcode","交易类型"),
    TRX_ID("trxid","收银宝交易单号"),
    INIT_AMT("initamt","原始下单金额"),
    TRX_AMT("trxamt","交易金额"),
    TRX_DATE("trxdate","交易请求日期"),
    PAY_TIME("paytime","交易完成时间"),
    CHNL_TRX_ID("chnltrxid","渠道流水号"),
    TRX_STATUS("trxstatus","交易结果码"),
    CUS_ID("cusid","商户编号"),
    TERM_NO("termno","终端编号"),
    TERM_BATCH_ID("termbatchid","终端批次号"),
    TERM_TRACE_NO("termtraceno","终端流水号"),
    TERM_REF_NUM("termrefnum","终端参考号"),
    TRX_RESERVED("trxreserved","业务关联内容"),
    SRC_TRX_ID("srctrxid","原交易流水"),
    CUS_ORDER_ID("cusorderid","业务流水"),
    ACCT("acct","交易账号"),
    FEE("fee","手续费"),
    SIGN_TYPE("signtype","签名类型"),
    CM_ID("cmid","渠道子商户号"),
    CHNL_ID("chnlid","渠道号"),
    CHNL_DATA("chnldata","渠道信息"),
    ACCT_TYPE("accttype","借贷标识"),
    BANK_CODE("bankcode","\t发卡行"),
    LOGO_NID("logonid","支付宝买家账号"),
    SIGN("sign","sign校验码"),
    TL_OPEN_ID("tlopenid","通联渠道侧OPENID");

    private final String code;

    private final String note;
}