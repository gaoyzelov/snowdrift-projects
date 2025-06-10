package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WithdrawQueryDto
 *
 * @author gaoye
 * @date 2025/06/06 15:43:58
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现查询参数")
public class WithdrawQueryDto extends RequestDto {

    @Schema(title = "商户请求号")
    private String requestNo;

    @Schema(title = "易宝提现订单号")
    private String orderNo;
}