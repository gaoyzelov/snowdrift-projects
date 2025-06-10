package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WithdrawApplyVo
 *
 * @author gaoye
 * @date 2025/06/06 14:44:16
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现申请返回参数")
public class WithdrawApplyVo extends ResponseVo {

    @Schema(title = "订单状态")
    private String status;

    @Schema(title = "易宝提现订单号")
    private String orderNo;
}