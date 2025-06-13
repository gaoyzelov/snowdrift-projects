package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * MerchantBalanceDto
 *
 * @author gaoye
 * @date 2025/06/10 11:12:36
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "平台账户余额查询参数")
public class MerchantBalanceDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;
}