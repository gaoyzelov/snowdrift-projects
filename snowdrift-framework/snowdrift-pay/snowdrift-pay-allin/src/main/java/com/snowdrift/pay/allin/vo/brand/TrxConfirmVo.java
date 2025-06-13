package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * TrxConfirmVo
 *
 * @author gaoye
 * @date 2025/06/10 11:39:16
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "交易确认结果")
public class TrxConfirmVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "会员编号")
    private String bizUserId;

    @Schema(title = "商户订单号")
    private String bizOrderNo;
}