package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

/**
 * WithdrawApplyDto
 *
 * @author gaoye
 * @date 2025/05/29 13:40:33
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "提现申请参数")
public class WithdrawApplyDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    /**
     * 若平台，填固定值：#yunBizUserId_B2C#
     */
    @Schema(title = "会员编号", required = true)
    @NotBlank(message = "会员编号不能为空")
    private String bizUserId = "#yunBizUserId_B2C#";

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String bizOrderNo;

    @Schema(title = "提现金额，单位：分", required = true)
    @NotNull(message = "提现金额不能为空")
    private Long amount;

    @Schema(title = "提现银行卡号", required = true)
    @NotBlank(message = "提现银行卡号不能为空")
    private String bankCardNo;

    @Schema(title = "提现发起IP", required = true)
    @NotBlank(message = "提现发起IP不能为空")
    private String consumerIp;

    @Schema(title = "提现附言,最大长度20")
    @Length(max = 20, message = "提现附言,最大长度20")
    private String summary;

    @Schema(title = "提现备注,最大长度50")
    @Length(max = 50, message = "提现备注,最大长度50")
    private String remark;

    @Schema(title = "交易验证方式,0-不验证，1-短信验证", required = true)
    @NotNull(message = "交易验证方式不能为空")
    @Range(min = 0, max = 1, message = "交易验证方式,0-不验证，1-短信验证")
    private Long validateType = 1L;

    @Schema(title = "手续费,单位分")
    private Long fee;
}