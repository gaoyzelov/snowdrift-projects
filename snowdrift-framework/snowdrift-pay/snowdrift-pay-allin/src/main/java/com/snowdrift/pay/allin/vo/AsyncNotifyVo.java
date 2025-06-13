package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * NotifyVo
 *
 * @author gaoye
 * @date 2025/06/05 13:36:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "异步通知结果")
public class AsyncNotifyVo implements Serializable {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;

    @Schema(title = "通联订单号")
    private String orderNo;

    @Schema(title = "支付方式")
    private String payMethod;

    @Schema(title = "支付状态")
    private String payStatus;

    @Schema(title = "交易方向")
    private String tradeDirection;

    @Schema(title = "交易金额")
    private String amount;

    @Schema(title = "渠道手续费")
    private String chanelFee;

    @Schema(title = "交易创建时间")
    private String createTime;

    @Schema(title = "交易完成时间")
    private String finishTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "门店编号")
    private String storeNo;

    @Schema(title = "通道交易类型")
    private String payInterfaceTrxCode;

    @Schema(title = "支付人账号")
    private String acct;

    @Schema(title = "借贷标志")
    private String acctType;

    @Schema(title = "终端授权码")
    private String termAuthNo;

    @Schema(title = "实付金额")
    private String trxAmt;

    @Schema(title = "支付渠道交易单号")
    private String chnlTrxId;

    @Schema(title = "是否微信订单预消费")
    private String isPreConsume;

    @Schema(title = "完成金额")
    private Long arrivalAmount;
}