package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SubOrderInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:03:03
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "子订单信息")
public class SubOrderInfo implements Serializable {

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "易宝统一订单号")
    private String uniqueOrderNo;

    @Schema(title = "子单商户订单号")
    private String orderId;

    @Schema(title = "订单单金额")
    private BigDecimal orderAmount;
}