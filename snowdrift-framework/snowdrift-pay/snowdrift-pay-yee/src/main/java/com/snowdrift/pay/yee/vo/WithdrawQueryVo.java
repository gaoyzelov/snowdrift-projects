package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * WithdrawQueryVo
 *
 * @author gaoye
 * @date 2025/06/06 15:46:06
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现查询结果")
public class WithdrawQueryVo extends ResponseVo {

    @Schema(title = "商户请求号")
    private String requestNo;

    @Schema(title = "易宝提现订单号")
    private String orderNo;

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "提现金额")
    private BigDecimal orderAmount;

    @Schema(title = "到账金额")
    private BigDecimal receiveAmount;

    @Schema(title = "扣账金额")
    private BigDecimal debitAmount;

    @Schema(title = "提现下单时间")
    private String orderTime;

    @Schema(title = "提现完成时间")
    private String finishTime;

    @Schema(title = "提现订单状态")
    private String status;

    @Schema(title = "提现失败原因")
    private String failReason;

    @Schema(title = "手续费承担方商编")
    private String feeUndertakerMerchantNo;

    @Schema(title = "手续费")
    private BigDecimal fee;

    @Schema(title = "到账类型")
    private String receiveType;

    @Schema(title = "开户名")
    private String accountName;

    @Schema(title = "银行账号")
    private String accountNo;

    @Schema(title = "开户行名称")
    private String bankName;

    @Schema(title = "开户行编码")
    private String bankCode;

    @Schema(title = "支行编码")
    private String branchBankCode;

    @Schema(title = "冲退标识")
    private Boolean isReversed;

    @Schema(title = "冲退时间")
    private String reverseTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "提现模式")
    private String withdrawModel;

    @Schema(title = "扣款银行账户号")
    private String debitBankAccountBookNo;

    @Schema(title = "扣款银行编码")
    private String debitBankCode;

    @Schema(title = "核验方式,PWD-密码核验")
    private String verifyType;
}