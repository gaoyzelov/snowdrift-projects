package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * BankCardAccount
 *
 * @author gaoye
 * @date 2025/06/06 15:10:20
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "提现卡")
public class BankCardAccount implements Serializable {

    @Schema(title = "银行卡类型,DEBIT_CARD-借记卡,ENTERPRISE_ACCOUNT-对公账户")
    private String bankCardType;

    @Schema(title = "开户名")
    private String accountName;

    @Schema(title = "开户行编码")
    private String bankCode;

    @Schema(title = "银行账号")
    private String accountNo;

    @Schema(title = "银行卡标识")
    private String bindCardId;

    @Schema(title = "支行编码")
    private String branchBankCode;
}