package com.snowdrift.pay.yee.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * AggregatePayDto
 *
 * @author gaoye
 * @date 2025/06/05 18:57:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "聚合扫码支付参数")
public class AggregateCodeDto extends RequestDto{

    @Schema(title = "商户收款请求号", required = true)
    @NotBlank(message = "商户收款请求号不能为空")
    private String orderId;

    @Schema(title = "订单金额，单位：元,两位小数，最低0.01", required = true)
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(title = "0.01", message = "订单金额最低0.01")
    private BigDecimal orderAmount;

    @Schema(title = "商品名称", required = true)
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /**
     * 请传入复合结构，例如{"WECHAT":"PRIVATE_EDUCATION","ALIPAY":"LARGE"}，其中
     * 渠道为微信（WECHAT）时，可选值ONLINE/OFFLINE/BAOXIAN/GONGYI/DC_SEPARATION/DIGITAL/REGISTRATION/PRIVATE_EDUCATION
     * 渠道为支付宝时（ALIPAY）时，可选值OFFLINE/LARGE/REGISTRATION
     * 可选项如下:
     * OFFLINE:线下
     * BAOXIAN:保险
     * GONGYI:公益
     * DC_SEPARATION:借贷分离
     * DIGITAL:数娱
     * REGISTRATION:报名(需要先优惠费率报名成功，否则会阻断交易)
     * PRIVATE_EDUCATION:民办教育
     * LARGE:特殊
     *
     * 示例值：{"WECHAT":"PRIVATE_EDUCATION","ALIPAY":"LARGE"}
     * 仅渠道类型为银联时无需传入，否则必填
     */
    @Schema(title = "场景,业务上必填")
    private String scene;

    @Schema(title = "token,对应参数通过易宝接口“交易下单”的响应获取")
    private String token;

    /**
     * 分账明细
     * fundProcessType为实时分账时，必传
     * JSON格式：
     * ledgerNo 分账接收方
     * amount 分账金额
     * 实时分账的情况下，分账商编无需传入收单商编，除去分给他人的金额，订单剩余可分账金额均会分账给收单商户。
     *
     * 示例值：[{"amount":"金额","ledgerNo":"分账商编","divideDetailDesc":"分账说明"}]
     */
    @Schema(title = "分账明细")
    private String divideDetail;

    @Schema(title = "支付媒介,PRECONSUME:预消费")
    private String payMedium;

    @Schema(title = "订单截止时间,格式为yyyy-MM-dd HH:mm:ss,默认1天")
    private String expiredTime;

    @Schema(title = "支付结果通知地址")
    private String notifyUrl;

    @Schema(title = "对账备注")
    private String memo;

    /**
     * DELAY_SETTLE:需要分账
     * REAL_TIME:不需要分账
     * REAL_TIME_DIVIDE:实时分账；需同时传入divideDetail
     */
    @Schema(title = "分账订单标记")
    private String fundProcessType;

    @Schema(title = "微信公众号ID")
    private String appId;

    @Schema(title = "渠道指定支付信息")
    private String channelSpecifiedInfo;

    @Schema(title = "渠道优惠信息")
    private String channelPromotionInfo;

    @Schema(title = "限制付款人信息")
    private String identityInfo;

    @Schema(title = "是否限制贷记卡,Y:仅借记卡可以支付,N:借贷记卡均可支付,不传默认为N")
    private String limitCredit;

    @Schema(title = "清算回调地址")
    private String csUrl;

    @Schema(title = "易宝营销信息")
    private String ypPromotionInfo;

    @Schema(title = "自定义参数信息")
    private String businessInfo;

    @Schema(title = "易宝营销产品信息")
    private String productInfo;

    @Schema(title = "分账通知地址")
    private String divideNotifyUrl;

    @Schema(title = "手续费补贴信息")
    private String feeSubsidyInfo;
}