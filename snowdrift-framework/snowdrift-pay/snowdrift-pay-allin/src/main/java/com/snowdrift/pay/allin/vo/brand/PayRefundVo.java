package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * PayRefundVo
 *
 * @author gaoye
 * @date 2025/06/05 11:06:26
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "退款申请返回结果")
public class PayRefundVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户退款订单号")
    private String bizOrderNo;

    @Schema(title = "原商户退款订单号")
    private String oldBizOrderNo;

    @Schema(title = "通联订单号")
    private String orderNo;

    @Schema(title = "支付状态")
    private String payStatus;

    @Schema(title = "支付状态描述")
    private String payStatusMsg;

    @Schema(title = "通道交易类型")
    private String payInterfacetrxcode;

    @Schema(title = "交易完成时间,yyyyMMddHHmmss")
    private String finishTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "渠道手续费")
    private String channelFee;
}