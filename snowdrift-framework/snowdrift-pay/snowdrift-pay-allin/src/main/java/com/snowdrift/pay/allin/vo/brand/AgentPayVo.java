package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * AgentPayVo
 *
 * @author gaoye
 * @date 2025/06/10 13:23:09
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "托管代付申请结果")
public class AgentPayVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;

    @Schema(title = "通联支付订单号")
    private String orderNo;

    @Schema(title = "支付状态")
    private String payStatus;

    @Schema(title = "支付状态描述")
    private String payStatusMsg;

    @Schema(title = "交易完成时间")
    private String finishTime;
}