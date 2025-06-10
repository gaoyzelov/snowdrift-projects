package com.snowdrift.pay.yee.vo;


import com.snowdrift.pay.yee.bo.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * OrderQueryVo
 *
 * @author gaoye
 * @date 2025/06/06 11:18:19
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "订单查询结果")
public class OrderQueryVo extends ResponseVo {

    @Schema(title = "发起方商编")
    private String parentMerchantNo;

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "商户收款请求号")
    private String orderId;

    @Schema(title = "易宝收款订单号")
    private String uniqueOrderNo;

    @Schema(title = "订单状态")
    private String status;

    @Schema(title = "订单金额,元")
    private BigDecimal orderAmount;

    @Schema(title = "用户支付金额,元")
    private BigDecimal payAmount;

    @Schema(title = "商户手续费,元")
    private BigDecimal merchantFee;

    @Schema(title = "用户手续费,元")
    private BigDecimal customerFee;

    @Schema(title = "支付成功时间")
    private String paySuccessDate;

    @Schema(title = "对账备注")
    private String memo;

    @Schema(title = "支付方式")
    private String payWay;

    @Schema(title = "支付授权token")
    private String token;

    @Schema(title = "分账订单标识")
    private String fundProcessType;

    @Schema(title = "银行订单号")
    private String bankOrderId;

    @Schema(title = "渠道订单号")
    private String channelOrderId;

    @Schema(title = "支付渠道")
    private String channel;

    @Schema(title = "用户实际支付金额")
    private BigDecimal realPayAmount;

    @Schema(title = "剩余可分账金额")
    private BigDecimal unSplitAmount;

    @Schema(title = "累计已退款金额")
    private BigDecimal totalRefundAmount;

    @Schema(title = "支付者信息")
    private PayerInfo payerInfo;

    @Schema(title = "渠道侧优惠列表")
    private List<ChannelPromotionInfos> channelPromotionInfo;

    @Schema(title = "易宝优惠列表")
    private List<PromotionInfo> ypPromotionInfo;

    @Schema(title = "终端信息")
    private String terminalInfo;

    @Schema(title = "信用卡分期实体")
    private InstallmentInfo installmentInfo;

    @Schema(title = "渠道拓展信息")
    private EnterprisePayInfo enterprisePayInfo;

    @Schema(title = "支付失败的code码")
    private String failCode;

    @Schema(title = "支付失败的原因")
    private String failReason;

    @Schema(title = "清算状态,SUCCESS-成功，PROCESSING-处理中")
    private String clearStatus;

    @Schema(title = "手续费出资详情列表")
    private List<FeeContributeInfo> feeContributeInfo;

    @Schema(title = "资金到账类型,REALTIME-实时，PREAUTH-预授权")
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
    private BigDecimal preAuthAmount;

    @Schema(title = "聚合商户号信息")
    private ChannelMerchantInfo channelMerchantInfo;

    @Schema(title = "清算成功时间")
    private String csSuccessDate;

    @Schema(title = "入账金额")
    private  BigDecimal ypSettleAmount;

    @Schema(title = "手续费承担商编")
    private String feeMerchantNo;

    @Schema(title = "手续费类型")
    private String feeType;

    @Schema(title = "清算核验渠道:NUCC-网联,UP-银联,OTHER-三方")
    private String outClearChannel;

    @Schema(title = "微信交易单号/支付宝订单号")
    private String channelTrxId;

    /**
     * INIT:处理中
     * FROZEN:已冻结
     * UN_FROZEN:已解冻
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

    @Schema(title = "原始可分账金额")
    private BigDecimal originalDivideAmount;

    @Schema(title = "累计已分账金额")
    private BigDecimal totalDivideAmount;

    @Schema(title = "外卡币种")
    private String currencyCode;

    @Schema(title = "外币金额")
    private BigDecimal deductionAmount;

    @Schema(title = "预授权方式")
    private String preAuthWay;

    @Schema(title = "微信appid")
    private String appID;

    @Schema(title = "手续费费率信息")
    private FeeRateInfo feeRateInfo;

    @Schema(title = "信用分请求号")
    private String creditOrderId;
}