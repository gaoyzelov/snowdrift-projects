package com.snowdrift.pay.yee.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


/**
 * OrderQueryDto
 *
 * @author gaoye
 * @date 2025/06/06 11:16:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "订单查询参数")
public class OrderQueryDto extends RequestDto {

    @Schema(title = "交易下单传入的商户收款请求号", required = true)
    @NotBlank(message = "商户收款请求号不能为空")
    private String orderId;
}