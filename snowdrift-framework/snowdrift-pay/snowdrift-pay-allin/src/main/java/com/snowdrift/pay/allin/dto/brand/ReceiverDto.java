package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverDto
 *
 * @author gaoye
 * @date 2025/06/03 17:24:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款方信息查询参数")
public class ReceiverDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;
}