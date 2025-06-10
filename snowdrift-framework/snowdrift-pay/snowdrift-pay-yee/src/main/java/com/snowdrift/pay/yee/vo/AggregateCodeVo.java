package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * AggregatePayVo
 *
 * @author gaoye
 * @date 2025/06/05 19:58:20
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "聚合扫码支付结果")
public class AggregateCodeVo extends ResponseVo {

    @Schema(title = "发起方商编")
    private String parentMerchantNo;

    @Schema(title = "商户订单号")
    private String merchantNo;

    @Schema(title = "订单二维码地址")
    private String orderId;

    @Schema(title = "订单二维码地址")
    private String qrCodeUrl;

    @Schema(title = "易宝收款订单号")
    private String uniqueOrderNo;
}