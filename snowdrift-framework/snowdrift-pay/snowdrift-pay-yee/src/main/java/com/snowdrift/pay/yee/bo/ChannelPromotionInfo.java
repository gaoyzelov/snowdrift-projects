package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ChannelPromotionInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:14:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "渠道优惠信息")
public class ChannelPromotionInfo implements Serializable {

    @Schema(title = "优惠活动ID")
    private String activityId;

    @Schema(title = "优惠券名称")
    private String promotionName;

    @Schema(title = "优惠券面额")
    private BigDecimal amount;

    /**
     * GLOBAL：微信全场券
     * SINGLE：微信单品券
     * ALIPAY_FIX_VOUCHER：支付宝全场代金券
     * ALIPAY_DISCOUNT_VOUCHER：支付宝折扣券
     * ALIPAY_ITEM_VOUCHER：支付宝单品优惠券
     * ALIPAY_CASH_VOUCHER：支付宝现金抵价券
     * ALIPAY_BIZ_VOUCHER：支付宝商家全场券
     */
    @Schema(title = "优惠范围")
    private String promotionScope;

    @Schema(title = "渠道出资金额")
    private String channelContribute;

    @Schema(title = "商户出资金额")
    private String merchantContribute;

    @Schema(title = "备注")
    private String memo;

    @Schema(title = "其他出资金额")
    private String otherContribute;

    @Schema(title = "优惠券ID")
    private String promotionId;

    @Schema(title = "优惠券类型")
    private String promotionType;
}