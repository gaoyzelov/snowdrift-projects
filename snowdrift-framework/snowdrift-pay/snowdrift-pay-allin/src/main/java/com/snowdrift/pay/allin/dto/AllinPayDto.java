package com.snowdrift.pay.allin.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * AllinPayDto
 *
 * @author gaoye
 * @date 2025/05/22 09:30:59
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "支付参数")
public class AllinPayDto implements Serializable {

    @Schema(title = "集团/代理商商户号")
    private String orgid;

    @Schema(title = "商户号", required = true)
    @NotBlank(message = "商户号不能为空")
    private String cusid;

    @Schema(title = "应用ID", required = true)
    @NotBlank(message = "应用ID不能为空")
    private String appid;

}