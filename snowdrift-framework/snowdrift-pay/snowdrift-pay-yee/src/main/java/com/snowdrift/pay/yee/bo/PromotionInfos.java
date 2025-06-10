package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * PromotionInfos
 *
 * @author gaoye
 * @date 2025/06/06 11:38:07
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "易宝优惠列表")
public class PromotionInfos extends PromotionInfo{

    @Schema(title = "营销订单号")
    private String subsidyOrderNo;

    @Schema(title = "出资方商编")
    private String contributeMerchant;

    @Schema(title = "营销状态")
    private String status;
}