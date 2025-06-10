package com.snowdrift.pay.yee.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * PayerInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:08:31
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "付款方信息")
public class PayerInfo implements Serializable {

    @Schema(title = "银行编号")
    private String bankId;

    @Schema(title = "账户名称")
    private String accountName;

    @Schema(title = "银行卡号")
    private String bankCardNo;

    @Schema(title = "手机号")
    private String mobilePhoneNo;

    /**
     * DEBIT：借记卡
     * CREDIT：贷记卡
     * CFT：微信零钱
     * QUASI_CREDIT：准贷记卡
     * PUBLIC_ACCOUNT：对公账户
     */
    @Schema(title = "银行卡类型")
    private String cardType;

    @Schema(title = "用户ID")
    private String userID;

    @Schema(title = "支付宝买家登录账号")
    private String buyerLogonId;

    @Schema(title = "记帐簿编号")
    private String ypAccountBookNo;

    @Schema(title = "商户ID")
    private String appID;

    @Schema(title = "微信/支付宝订单号")
    private String channelTrxId;

    @Schema(title = "银行附言")
    private String bankPostscript;
}