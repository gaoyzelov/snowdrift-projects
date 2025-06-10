package com.snowdrift.pay.yee.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * FeeRateInfo
 *
 * @author gaoye
 * @date 2025/06/06 13:04:47
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "手续费费率信息")
public class FeeRateInfo implements Serializable {

    @Schema(title = "计费策略")
    private String rateType;

    @Schema(title = "手续费收取方式")
    private String paymentMethod;

    @Schema(title = "周期区间类型")
    private String ladderCycleType;

    @Schema(title = "费率公式")
    private List<FeeRateFormula> feeRateFormulaList;
}