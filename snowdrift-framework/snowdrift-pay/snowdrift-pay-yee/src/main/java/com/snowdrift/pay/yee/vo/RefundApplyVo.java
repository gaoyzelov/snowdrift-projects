package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * RefundApplyVo
 *
 * @author gaoye
 * @date 2025/06/06 13:36:45
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "退款申请响应")
public class RefundApplyVo extends ResponseVo {

    @Schema(title = "发起方商编")
    private String parentMerchantNo;

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "商户收款订单号")
    private String orderId;

    @Schema(title = "商户退款请求号")
    private String refundRequestId;

    @Schema(title = "易宝退款订单号")
    private String uniqueRefundNo;

    @Schema(title = "退款订单状态")
    private String status;

    @Schema(title = "退款申请金额")
    private String refundAmount;

    @Schema(title = "refundRequestDate")
    private String refundRequestDate;

    @Schema(title = "退还商户手续费")
    private String refundMerchantFee;

    @Schema(title = "退款资金来源信息")
    private String refundAccountDetail;

    @Schema(title = "扣账时间")
    private String refundCsFinishDate;
}