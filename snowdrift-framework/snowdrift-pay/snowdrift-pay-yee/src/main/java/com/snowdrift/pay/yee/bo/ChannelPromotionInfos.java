package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * ChannelPromotionInfos
 *
 * @author gaoye
 * @date 2025/06/06 11:37:30
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "渠道优惠信息")
public class ChannelPromotionInfos extends ChannelPromotionInfo{

    @Schema(title = "优惠券退回金额")
    private BigDecimal amountRefund;
}