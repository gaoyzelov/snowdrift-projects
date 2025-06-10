package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * RefundApplyDto
 *
 * @author gaoye
 * @date 2025/06/06 13:30:39
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "退款申请参数")
public class RefundApplyDto extends RequestDto {

    @Schema(title = "原收款交易对应的商户收款请求号", required = true)
    @NotBlank(message = "原收款交易对应的商户收款请求号不能为空")
    private String orderId;

    @Schema(title = "商户退款请求号", required = true)
    @NotBlank(message = "商户退款请求号不能为空")
    private String refundRequestId;

    @Schema(title = "退款金额", required = true)
    @NotBlank(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @Schema(title = "收款交易对应的易宝收款订单号")
    private String uniqueOrderNo;

    @Schema(title = "退款原因")
    private String description;

    @Schema(title = "对账备注")
    private String memo;

    @Schema(title = "退款资金来源")
    private String refundAccountType;

    @Schema(title = "退款结果回调url")
    private String notifyUrl;

    @Schema(title = "分账归集明细")
    private String divideBackDetail;

    @Schema(title = "终端信息")
    private String terminalInfo;

    @Schema(title = "易宝退款营销")
    private String ypPromotionRefundInfo;
}