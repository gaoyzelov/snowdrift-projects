package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * FreezeMoneyVo
 *
 * @author gaoye
 * @date 2025/06/10 13:15:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "冻结金额返回结果")
public class FreezeMoneyVo extends AllinBrandVo{

    @Schema(title = "冻结金额订单号")
    private String bizFreezenNo;

    @Schema(title = "冻结金额，单位：分")
    private Long amount;
}