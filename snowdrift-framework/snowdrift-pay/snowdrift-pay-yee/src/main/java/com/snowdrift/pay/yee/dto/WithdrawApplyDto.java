package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * WithdrawApplyDto
 *
 * @author gaoye
 * @date 2025/06/06 14:43:52
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现申请参数")
public class WithdrawApplyDto extends RequestDto {

    @Schema(title = "商户请求号", required = true)
    @NotBlank(message = "商户请求号不能为空")
    private String requestNo;

    @Schema(title = "到账类型,REAL_TIME-实时，TWO_HOUR-2小时，NEXT_DAY-次日到账", required = true)
    @NotBlank(message = "到账类型不能为空")
    private String receiveType;

    @Schema(title = "提现金额，单位：元", required = true)
    @NotNull(message = "提现金额不能为空")
    private BigDecimal orderAmount;

    @Schema(title = "提现卡ID,提现账号至少填写一个")
    private String bankCardId;

    @Schema(title = "提现账号,提现卡ID至少填写一个")
    private String bankAccountNo;

    @Schema(title = "回调通知地址")
    private String notifyUrl;

    @Schema(title = "银行附言")
    private String remark;

    /**
     * PC:电脑
     * PHONE:手机
     * PAD:平板
     * NFC:可穿戴设备
     * DTV:数字电视
     * MPOS:条码支付受理终端
     * OTHER:其他
     */
    @Schema(title = "终端类型")
    private String terminalType;

    /**
     * OUTSIDE:外扣
     * OUT_TO_IN:外扣转内扣
     * INSIDE:内扣
     */
    @Schema(title = "手续费收取方式")
    private String feeDeductType;

    /**
     * FUND_ACCOUNT:资金账户
     * MARKET_ACCOUNT:营销账户
     * FEE_ACCOUNT:手续费账户
     */
    @Schema(title = "账户类型")
    private String accountType;

    @Schema(title = "设备mac地址")
    private String macAddress;

    /**
     * INNER_ACCOUNT_WITHDRAW:易宝内部账户模式
     * BANK_ACCOUNT_WITHDRAW:银行清分银行账户模式
     */
    @Schema(title = "提现模式")
    private String withdrawModel;

    @Schema(title = "扣款银行账户号")
    private String debitBankAccountBookNo;

    @Schema(title = "核验方式:PWD-密码")
    private String verifyType;

    @Schema(title = "核验值")
    private String verifytitle;
}