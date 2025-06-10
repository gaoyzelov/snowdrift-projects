package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * RequestDto
 *
 * @author gaoye
 * @date 2025/06/06 14:30:54
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
public class RequestDto implements Serializable {

    @Schema(title = "发起方商户编号", required = true)
    @NotBlank(message = "发起方商户编号不能为空")
    private String parentMerchantNo;

    @Schema(title = "商户编号", required = true)
    @NotBlank(message = "商户编号不能为空")
    private String merchantNo;
}