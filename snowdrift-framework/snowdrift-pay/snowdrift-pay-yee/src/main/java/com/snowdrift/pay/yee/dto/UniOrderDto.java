package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * UnionPayDto
 *
 * @author gaoye
 * @date 2025/06/06 10:34:53
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "聚合支付统一下单")
public class UniOrderDto extends RequestDto {

    @Schema(title = "支付方式", required = true)
    @NotBlank(message = "支付方式不能为空")
    private String payWay;

    @Schema(title = "渠道类型", required = true)
    @NotBlank(message = "渠道类型不能为空")
    private String channel;

    @Schema(title = "用户IP", required = true)
    @NotBlank(message = "用户IP不能为空")
    private String userIp;

    @Schema(title = "商户收款请求号", required = true)
    @NotBlank(message = "商户收款请求号不能为空")
    private String orderId;

    @Schema(title = "订单金额", required = true)
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(title = "页面回调地址", required = true)
    @NotBlank(message = "页面回调地址不能为空")
    private String redirectUrl;

    @Schema(title = "场景,不是银联时必填")
    private String scene;

    @Schema(title = "微信公众号ID/微信小程序ID/支付宝小程序ID商家的公众号id")
    private String appId;

    @Schema(title = "用户ID用户标识")
    private String userId;

    @Schema(title = "渠道指定支付信息，json格式")
    private String channelSpecifiedInfo;

    @Schema(title = "易宝订单号")
    private String uniqueOrderNo;

    @Schema(title = "token,商户先下单后支付时，与uniqueOrderNo二选一必填")
    private String token;

    @Schema(title = "银行编码")
    private String bankCode;

    @Schema(title = "终端ID")
    private String terminalId;

    @Schema(title = "分账明细")
    private String divideDetail;

    @Schema(title = "协议ID")
    private String agreementId;

    @Schema(title = "信用分请求号")
    private String creditOrderId;

    @Schema(title = "支付媒介，PRECONSUME:预消费")
    private String payMedium;

    @Schema(title = "订单截止时间")
    private String expiredTime;

    @Schema(title = "支付结果通知地址")
    private String notifyUrl;

    @Schema(title = "对账备注")
    private String memo;

    @Schema(title = "商品名称")
    private String goodsName;

    @Schema(title = "分账订单标记")
    private String fundProcessType;

    @Schema(title = "渠道优惠信息")
    private String channelPromotionInfo;

    @Schema(title = "限制付款人信息")
    private String identityInfo;

    @Schema(title = "是否限制贷记卡")
    private String limitCredit;

    @Schema(title = "清算回调地址")
    private String csUrl;

    @Schema(title = "合作银行信息")
    private String accountLinkInfo;

    @Schema(title = "易宝营销信息")
    private String ypPromotionInfo;

    @Schema(title = "自定义参数信息")
    private String businessInfo;

    @Schema(title = "用户授权码")
    private String userAuthCode;

    @Schema(title = "渠道活动信息")
    private String channelActivityInfo;

    @Schema(title = "商家终端场景信息")
    private String terminalSceneInfo;

    @Schema(title = "记账簿编号")
    private String ypAccountBookNo;

    @Schema(title = "终端信息")
    private String terminalInfo;

    @Schema(title = "易宝营销产品信息")
    private String productInfo;

    @Schema(title = "分账结果通知地址")
    private String divideNotifyUrl;

    @Schema(title = "手续费补贴信息")
    private String feeSubsidyInfo;
}