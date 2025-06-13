package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * MerchantBalanceVo
 *
 * @author gaoye
 * @date 2025/06/10 11:13:20
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "平台账户余额查询结果")
public class MerchantBalanceVo extends AllinBrandVo {

    @Schema(title = "总额")
    private Long allAmount;

    @Schema(title = "冻结额")
    private String freezeAmount;
}