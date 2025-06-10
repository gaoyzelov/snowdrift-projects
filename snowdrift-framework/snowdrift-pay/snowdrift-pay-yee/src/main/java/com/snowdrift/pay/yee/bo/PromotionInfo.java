package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * PromotionInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:04:54
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "营销信息")
public class PromotionInfo implements Serializable {

    @Schema(title = "营销类型")
    private String type;

    @Schema(title = "营销活动编号")
    private String marketNo;

    @Schema(title = "营销金额")
    private String amount;

    @Schema(title = "优惠券编号")
    private String couponNo;
}