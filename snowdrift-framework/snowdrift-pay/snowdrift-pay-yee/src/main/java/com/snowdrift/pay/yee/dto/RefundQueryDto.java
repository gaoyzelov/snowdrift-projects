package com.snowdrift.pay.yee.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * RefundQueryDto
 *
 * @author gaoye
 * @date 2025/06/06 13:49:41
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "查询退款参数")
public class RefundQueryDto extends RequestDto {

    @Schema(title = "商户收款订单号", required = true)
    @NotBlank(message = "商户收款订单号不能为空")
    private String orderId;

    @Schema(title = "商户退款请求号", required = true)
    @NotBlank(message = "商户退款请求号不能为空")
    private String refundRequestId;

    @Schema(title = "易宝退款订单号")
    private String uniqueRefundNo;
}