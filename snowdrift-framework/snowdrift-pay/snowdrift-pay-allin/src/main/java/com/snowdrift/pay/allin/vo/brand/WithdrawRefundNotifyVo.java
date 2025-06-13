package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * WithdrawRefundNotifyVo
 *
 * @author gaoye
 * @date 2025/06/10 13:04:33
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "提现退票订单通知")
public class WithdrawRefundNotifyVo implements Serializable {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "会员编号")
    private String bizUserId;

    @Schema(title = "系统提现订单号")
    private String orderNo;

    @Schema(title = "商户提现订单号")
    private String bizOrderNo;

    @Schema(title = "退票状态,4-交易成功")
    private String status;

    @Schema(title = "退票金额，单位：分")
    private Long amount;

    @Schema(title = "退票手续费，单位：分")
    private Long fee;

    @Schema(title = "退票完成时间")
    private String returnedCheque;

    @Schema(title = "退票原因")
    private String returnedChequeMsg;
}