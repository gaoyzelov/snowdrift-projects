package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * TransferQueryDto
 *
 * @author gaoye
 * @date 2025/05/29 14:49:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "转账汇款交易结果查询参数")
public class TransferQueryDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String bizOrderNo;
}