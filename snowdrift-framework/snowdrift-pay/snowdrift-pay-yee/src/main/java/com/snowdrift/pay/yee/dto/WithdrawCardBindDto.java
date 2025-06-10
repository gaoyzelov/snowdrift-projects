package com.snowdrift.pay.yee.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


/**
 * WithdrawCardBindDto
 *
 * @author gaoye
 * @date 2025/06/06 15:23:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现卡绑定参数")
public class WithdrawCardBindDto extends RequestDto {

    @Schema(title = "银行卡类型", required = true)
    @NotBlank(message = "银行卡类型不能为空")
    private String bankCardType;

    @Schema(title = "银行卡号", required = true)
    @NotBlank(message = "银行卡号不能为空")
    private String accountNo;

    @Schema(title = "开户行编码")
    private String bankCode;

    @Schema(title = "银行支行编码")
    private String branchCode;
}