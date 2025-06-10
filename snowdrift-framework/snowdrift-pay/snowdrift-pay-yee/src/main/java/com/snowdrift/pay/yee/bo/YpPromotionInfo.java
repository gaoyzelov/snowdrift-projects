package com.snowdrift.pay.yee.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * YpPromotionInfo
 *
 * @author gaoye
 * @date 2025/06/06 14:07:06
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "易宝侧优惠退回列表")
public class YpPromotionInfo implements Serializable {

    @Schema(title = "活动编号")
    private String marketNo;

    @Schema(title = "活动退回金额")
    private String ypRefundAmount;

    @Schema(title = "自定义营销活动类型")
    private String type;

    @Schema(title = "优惠券编号")
    private String couponNo;
}