package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * TransferQueryVo
 *
 * @author gaoye
 * @date 2025/05/29 14:51:16
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "转账汇款交易结果查询响应数据")
public class TransferQueryVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;

    @Schema(title = "通联订单号")
    private String orderNo;

    @Schema(title = "转账单号")
    private String transferOrderNo;

    @Schema(title = "转账汇款状态")
    private String status;

    @Schema(title = "转账汇款状态描述")
    private String statusMsg;

    @Schema(title = "交易金额，单位：分")
    private Long amount;

    @Schema(title = "交易完成时间")
    private String finishTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "提现状态")
    private String withdrawStatus;

    @Schema(title = "提现列表")
    private List<WithdrawVo> withdrawList;
}