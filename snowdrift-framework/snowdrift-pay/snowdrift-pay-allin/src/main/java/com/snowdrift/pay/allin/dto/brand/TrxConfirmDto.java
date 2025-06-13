package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * TrxConfirmDto
 *
 * @author gaoye
 * @date 2025/06/10 11:38:53
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "确认交易参数")
public class TrxConfirmDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "会员编号", required = true)
    @NotBlank(message = "会员编号不能为空")
    private String bizUserId;

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String bizOrderNo;

    @Schema(title = "验证码", required = true)
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    @Schema(title = "交易发起IP", required = true)
    @NotBlank(message = "交易发起IP不能为空")
    private String consumerIp;

    @Schema(title = "订单类型,00-支付/退款订单,01-提现订单", required = true)
    @NotBlank(message = "订单类型不能为空")
    private String orderType;
}