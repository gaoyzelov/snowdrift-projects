package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * UnfreezeMoneyVo
 *
 * @author gaoye
 * @date 2025/06/10 13:16:36
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "解冻金额返回结果")
public class UnfreezeMoneyVo extends AllinBrandVo{

    @Schema(title = "冻结金额订单号")
    private String bizFreezenNo;

    @Schema(title = "冻结金额，单位：分")
    private Long amount;
}