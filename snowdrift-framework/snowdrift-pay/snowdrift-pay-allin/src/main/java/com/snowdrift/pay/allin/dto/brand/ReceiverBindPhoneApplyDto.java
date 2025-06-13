package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverSmsCodeDto
 *
 * @author gaoye
 * @date 2025/06/05 09:09:01
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款方短信认证触发参数")
public class ReceiverBindPhoneApplyDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "手机号码", required = true)
    @NotBlank(message = "手机号码不能为空")
    private String phone;
}