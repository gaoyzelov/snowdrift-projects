package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * TrxQueryVo
 *
 * @author gaoye
 * @date 2025/06/05 11:31:12
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "交易结果查询返回结果")
public class TrxQueryVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;

    @Schema(title = "原商户订单号")
    private String oldBizOrderNo;

    @Schema(title = "通联订单号")
    private String orderNo;

    @Schema(title = "支付方式")
    private String payMethod;

    @Schema(title = "支付状态")
    private String payStatus;

    @Schema(title = "支付状态描述")
    private String payStatusMsg;

    @Schema(title = "交易金额")
    private Long amount;

    @Schema(title = "交易创建时间,yyyyMMddHHmmss")
    private String createTime;

    @Schema(title = "交易完成时间,yyyyMMddHHmmss")
    private String finishTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "交易方向，01-消费，02-退款")
    private String tradeDirection;

    @Schema(title = "订单详情")
    private String orderDetails;

    @Schema(title = "分账详情")
    private String splitDetails;

    @Schema(title = "渠道手续费")
    private String channelFee;

    @Schema(title = "渠道详细信息")
    private String chnlInfo;

    @Schema(title = "完成金额")
    private Long arrivalAmount;
}