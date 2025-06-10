package com.snowdrift.pay.yee.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * FeeContributeInfo
 *
 * @author gaoye
 * @date 2025/06/06 11:44:52
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "手续费出资详情")
public class FeeContributeInfo implements Serializable {

    @Schema(title = "手续费出资商编")
    private String merchantNo;

    @Schema(title = "手续费出资金额")
    private String amount;

    /**
     * TRADE: 交易手续费
     * SUBSIDY: 补贴手续费
     * REFUND: 退款退手续费费
     * RETURN_SUBSIDY: 退款退补贴手续费
     */
    @Schema(title = "手续费出资类型")
    private String type;

    /**
     * YEEPAY: 易宝
     * MERCHANT: 商户
     * OTHER: 其他
     */
    @Schema(title = "出资方类型")
    private String marketSource;
}