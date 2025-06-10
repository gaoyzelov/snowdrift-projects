package com.snowdrift.pay.yee.vo;


import com.snowdrift.pay.yee.bo.BankPromotionInfo;
import com.snowdrift.pay.yee.bo.YpPromotionInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * RefundQueryVo
 *
 * @author gaoye
 * @date 2025/06/06 13:52:15
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "查询退款结果")
public class RefundQueryVo extends ResponseVo {

    @Schema(title = "发起方商编")
    private String parentMerchantNo;

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "商户收款请求号")
    private String orderId;

    @Schema(title = "商户退款请求号")
    private String refundRequestId;

    @Schema(title = "退款申请金额")
    private String uniqueOrderNo;

    @Schema(title = "退款金额")
    private BigDecimal refundAmount;

    @Schema(title = "退回商户手续费金额")
    private BigDecimal returnMerchantFee;

    @Schema(title = "退款状态")
    private String status;

    @Schema(title = "退款原因的简要描述")
    private String description;

    @Schema(title = "退款受理时间")
    private String refundRequestDate;

    @Schema(title = "退款成功时间")
    private String refundSuccessDate;

    @Schema(title = "退款失败原因")
    private String failReason;

    @Schema(title = "实际退款金额")
    private BigDecimal realRefundAmount;

    @Schema(title = "用户实退金额")
    private BigDecimal cashRefundFee;

    @Schema(title = "渠道侧优惠退回列表")
    private List<BankPromotionInfo> bankPromotionInfoDTOList;

    @Schema(title = "易宝侧优惠退回列表")
    private List<YpPromotionInfo> ypPromotionInfoDTOList;

    @Schema(title = "退款资金来源信息")
    private String refundAccountDetail;

    @Schema(title = "退款入账信息")
    private String channelReceiverInfo;

    @Schema(title = "扣账时间")
    private String refundCsFinishDate;

    @Schema(title = "退款银行订单号")
    private String bankRefundOrderNo;

    @Schema(title = "退款银行流水号")
    private String bankRefundOrderId;

    @Schema(title = "支付方式")
    private String paymentMethod;

    @Schema(title = "商户账户扣账金额")
    private BigDecimal disAccountAmount;

    @Schema(title = "原单订单管控状态")
    private String orgFundControlCsStatus;

    @Schema(title = "原单解冻完成时间")
    private String orgCsUnFrozenCompleteDate;

    @Schema(title = "原交易订单收方一级基础产品码")
    private String orgBasicsProductFirst;

    @Schema(title = "原交易订单收方二级基础产品码")
    private String orgBasicsProductSecond;

    @Schema(title = "原交易订单收方三级基础产品码")
    private String orgBasicsProductThird;
}