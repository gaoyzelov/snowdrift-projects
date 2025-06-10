package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * UnionOrderVo
 *
 * @author gaoye
 * @date 2025/06/06 10:54:35
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "聚合支付统一下单结果")
public class UniOrderVo extends ResponseVo {

    @Schema(title = "商户收款请求号")
    private String orderId;

    @Schema(title = "易宝收款订单号")
    private String uniqueOrderNo;

    @Schema(title = "银行订单号")
    private String bankOrderId;

    @Schema(title = "预支付标识信息")
    private String prePayTn;
}