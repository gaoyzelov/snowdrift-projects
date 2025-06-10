package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * FeeRateFormula
 *
 * @author gaoye
 * @date 2025/06/06 13:06:18
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "费率公式")
public class FeeRateFormula implements Serializable {

    @Schema(title = "单笔区间起始值")
    private String ladderMin;

    @Schema(title = "单笔区间结束值")
    private String ladderMax;

    @Schema(title = "周期区间起始值")
    private String periodLadderMin;

    @Schema(title = "周期区间结束值")
    private String periodLadderMax;

    @Schema(title = "计费策略")
    private String rateType;

    @Schema(title = "单笔百分比")
    private String percentRate;

    @Schema(title = "单笔固定值")
    private String fixedRate;

    @Schema(title = "封顶值")
    private String maxRate;

    @Schema(title = "保底值")
    private String minRate;
}