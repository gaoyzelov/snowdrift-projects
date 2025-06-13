package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * TrxQueryDto
 *
 * @author gaoye
 * @date 2025/06/05 11:27:58
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "交易结果查询参数")
public class TrxQueryDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String bizOrderNo;

    @Schema(title = "渠道订单号")
    private String sybChannelOrderNo;

    @Schema(title = "订单类型,00-支付/退款，01-提现，02-代付",required = true)
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    @Schema(title = "查询模式,0-基础查询，1-详细信息查询")
    private Long fieldQueryType;
}