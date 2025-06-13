package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * SplitRefundDetail
 *
 * @author gaoye
 * @date 2025/06/05 11:03:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "分账退款详情")
public class SplitRefundDetail implements Serializable {

    @Schema(title = "分账会员编号", required = true)
    @NotBlank(message = "分账会员编号不能为空")
    private String splitBizUserId;

    @Schema(title = "分账退款金额，单位：分", required = true)
    @NotBlank(message = "分账退款金额不能为空")
    private Long splitAmount;

    @Schema(title = "备注", required = true)
    @NotBlank(message = "备注不能为空")
    private String splitRemark;

    @Schema(title = "代付订单收款人")
    private String receiver;

    @Schema(title = "通联代付订单号")
    private String splitOrderNo;

    @Schema(title = "退款金额，单位：分")
    private Long refundAmount;
}