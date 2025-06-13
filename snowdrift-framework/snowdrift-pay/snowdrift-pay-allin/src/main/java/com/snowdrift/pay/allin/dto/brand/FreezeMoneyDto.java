package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * FreezeMoneyDto
 *
 * @author gaoye
 * @date 2025/06/10 13:11:57
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "冻结金额参数")
public class FreezeMoneyDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "会员编号",required = true)
    @NotBlank(message = "会员编号不能为空")
    private String bizUserId;

    @Schema(title = "冻结金额订单号",required = true)
    @NotBlank(message = "冻结金额订单号不能为空")
    private String bizFreezenNo;

    @Schema(title = "冻结金额，单位：分",required = true)
    @NotNull(message = "冻结金额不能为空")
    private Long amount;
}