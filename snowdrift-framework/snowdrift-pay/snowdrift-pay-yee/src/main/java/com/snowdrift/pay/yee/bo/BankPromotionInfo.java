package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * BankPromotionInfo
 *
 * @author gaoye
 * @date 2025/06/06 14:05:25
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "渠道侧优惠退回列表")
public class BankPromotionInfo implements Serializable {

    @Schema(title = "优惠券编码")
    private String promotionId;

    @Schema(title = "优惠券名称")
    private String promotionName;

    @Schema(title = "优惠券退回金额")
    private BigDecimal amountRefund;

    @Schema(title = "优惠券活动id")
    private String activityId;

    @Schema(title = "渠道出资")
    private String channelContribute;

    @Schema(title = "商户出资")
    private String merchantContribute;

    @Schema(title = "其他出资")
    private String otherContribute;

    @Schema(title = "备注")
    private String memo;

    @Schema(title = "优惠券类型")
    private String promotionType;

    @Schema(title = "优惠范围")
    private String promotionScope;

}