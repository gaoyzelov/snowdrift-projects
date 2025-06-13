package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * PayApplyVo
 *
 * @author gaoye
 * @date 2025/06/05 10:19:54
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "支付申请返回参数")
public class PayApplyVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;

    @Schema(title = "通联支付订单号")
    private String orderNo;

    @Schema(title = "支付信息")
    private String payInfo;

    @Schema(title = "支付状态")
    private String payStatus;

    @Schema(title = "支付状态描述")
    private String payStatusMsg;

    @Schema(title = "通道交易类型")
    private String payInterfacetrxcode;

    @Schema(title = "支付完成时间")
    private String finishTime;

    @Schema(title = "备注")
    private String remark;
}