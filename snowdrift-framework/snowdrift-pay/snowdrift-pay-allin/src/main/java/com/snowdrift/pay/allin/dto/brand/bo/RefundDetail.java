package com.snowdrift.pay.allin.dto.brand.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * RefundDetail
 *
 * @author gaoye
 * @date 2025/06/05 11:02:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "退款详情")
public class RefundDetail implements Serializable {

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "退款金额", required = true)
    @NotNull(message = "退款金额不能为空")
    private Long amount;

    @Schema(title = "账户集编号")
    private String accountSetNo;
}