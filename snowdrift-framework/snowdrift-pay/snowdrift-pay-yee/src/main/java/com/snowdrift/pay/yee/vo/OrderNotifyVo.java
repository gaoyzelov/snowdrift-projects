package com.snowdrift.pay.yee.vo;


import com.snowdrift.pay.yee.bo.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * OrderNotifyVo
 *
 * @author gaoye
 * @date 2025/06/06 09:41:52
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "订单通知结果")
public class OrderNotifyVo implements INotifyVo {

    @Schema(title = "交易发起方的商编")
    private String parentMerchantNo;

    @Schema(title = "收款商户编号")
    private String merchantNo;

    @Schema(title = "渠道订单号")
    private String channelOrderId;

    @Schema(title = "交易下单传入的商户收款请求号")
    private String orderId;

    @Schema(title = "易宝订单号")
    private String uniqueOrderNo;

    @Schema(title = "支付成功时间")
    private String paySuccessDate;

    @Schema(title = "支付渠道")
    private String channel;

    @Schema(title = "支付方式")
    private String payWay;

    @Schema(title = "单位：元")
    private String orderAmount;

    @Schema(title = "支付金额，单位：元")
    private String payAmount;

    @Schema(title = "用户实际支付金额")
    private String realPayAmount;

    @Schema(title = "支付结果，SUCCESS-订单支付成功")
    private String status;

    @Schema(title = "支付失败Code码")
    private String failCode;

    @Schema(title = "支付失败原因")
    private String failReason;

    /**
     * REALTIME:实时
     * PREAUTH:预授权
     */
    @Schema(title = "资金到账类型")
    private String tradeType;

    /**
     * WAITPREAUTH:等待预授权
     * PREAUTH:预授权
     * PREAUTHREPEAL:预授权撤销
     * PREAUTHCOMPLETE:预授权完成
     * PREAUTHFAIL:预授权失败
     */
    @Schema(title = "预授权状态")
    private String preAuthStatus;

    @Schema(title = "预授权金额")
    private String preAuthAmount;

    @Schema(title = "微信/支付宝订单号")
    private String channelTrxId;

    @Schema(title = "银行订单号")
    private String bankOrderId;

    /**
     * INIT: 处理中
     * FROZEN: 已冻结
     * UN_FROZEN: 已解冻(一般不会出现，仅当支付结果通知滞后以后会小概率出现)
     * 该字段为INIT和FROZEN时，均可认为该笔订单为冻结状态
     */
    @Schema(title = "订单管控状态")
    private String fundControlCsStatus;

    @Schema(title = "管控订单解冻时间")
    private String csUnFrozenCompleteDate;

    @Schema(title = "收方一级基础产品码")
    private String basicsProductFirst;

    @Schema(title = "收方二级基础产品码")
    private String basicsProductSecond;

    @Schema(title = "收方三级基础产品码")
    private String basicsProductThird;

    @Schema(title = "对账备注")
    private String memo;

    @Schema(title = "子商户名称")
    private String merchantName;

    /**
     * NUCC-网联
     * UP-银联
     * OTHER-三方
     */
    @Schema(title = "清算核验渠道")
    private String outClearChannel;

    @Schema(title = "子订单列表")
    private List<SubOrderInfo> subOrderInfoList;

    @Schema(title = "易宝营销信息")
    private PromotionInfo ypPromotionInfo;

    @Schema(title = "付款方信息")
    private PayerInfo payerInfo;

    @Schema(title = "渠道优惠信息")
    private ChannelPromotionInfo channelPromotionInfo;

    @Schema(title = "分期信息")
    private InstallmentInfo installmentInfo;

    @Schema(title = "终端信息")
    private TerminalInfo terminalInfo;
}