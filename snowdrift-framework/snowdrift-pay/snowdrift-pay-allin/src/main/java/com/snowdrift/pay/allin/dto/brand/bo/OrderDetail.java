package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * OrderDetail
 *
 * @author gaoye
 * @date 2025/06/05 10:10:26
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "订单详情")
public class OrderDetail implements Serializable {

    @Schema(title = "交易类型", required = true)
    @NotBlank(message = "交易类型不能为空")
    private String tradeType;

    @Schema(title = "金额,单位：分", required = true)
    @NotNull(message = "金额不能为空")
    private Long amount;
}