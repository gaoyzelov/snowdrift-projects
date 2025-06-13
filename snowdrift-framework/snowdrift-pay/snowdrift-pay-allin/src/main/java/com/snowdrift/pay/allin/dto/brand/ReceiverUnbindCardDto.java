package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverUnbindCardDto
 *
 * @author gaoye
 * @date 2025/06/10 10:04:40
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "收款方银行卡解绑")
public class ReceiverUnbindCardDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号",required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "银行卡号",required = true)
    @NotBlank(message = "银行卡号不能为空")
    private String bankCardNo;
}