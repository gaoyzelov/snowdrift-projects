package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * PaySplit
 *
 * @author gaoye
 * @date 2025/06/05 10:12:35
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "支付分账参数")
public class PaySplit implements Serializable {

    @Schema(title = "分账明细", required = true)
    private List<SplitDetail> splitDetails;


    @Data
    @Schema(title = "分账明细")
    public static class SplitDetail implements Serializable {

        @Schema(title = "分账会员编号", required = true)
        @NotBlank(message = "分账会员编号不能为空")
        private String splitBizUserId;

        @Schema(title = "分账金额", required = true)
        @NotNull(message = "分账金额不能为空")
        private Long splitAmount;

        @Schema(title = "分账备注")
        private String splitRemark;

        @Schema(title = "分账场景")
        private String splitScene;
    }
}