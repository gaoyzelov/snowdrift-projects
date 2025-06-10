package com.snowdrift.pay.yee.vo;


import com.snowdrift.pay.yee.bo.BankPromotionInfo;
import com.snowdrift.pay.yee.bo.YpPromotionInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * RefundNotifyVo
 *
 * @author gaoye
 * @date 2025/06/06 13:42:23
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "退款结果通知")
public class RefundNotifyVo implements INotifyVo {

    @Schema(title = "收款交易对应的商户收款请求单号")
    private String orderId;

    @Schema(title = "收款交易对应在易宝的收款单号")
    private String uniqueOrderNo;

    @Schema(title = "退款请求号")
    private String refundRequestId;

    @Schema(title = "商户退款请求对应在易宝的退款单号")
    private String uniqueRefundNo;

    @Schema(title = "交易发起方商编")
    private String parentMerchantNo;

    @Schema(title = "收款商户商编")
    private String merchantNo;

    @Schema(title = "退款金额")
    private String refundAmount;

    @Schema(title = "退款状态")
    private String status;

    @Schema(title = "只有退款失败时回传")
    private String errorMessage;

    @Schema(title = "退款成功时间")
    private String refundSuccessDate;

    @Schema(title = "当支付方式为微信/支付宝/云闪付且参加渠道侧优惠时，退款可能退优惠，此字段为扣除优惠后实际退回用户金额")
    private String cashRefundFee;

    @Schema(title = "易宝侧优惠退回列表")
    private List<YpPromotionInfo> ypPromotionInfoDTOList;

    @Schema(title = "用户付手续费场景下,实际退款金额=退款金额 退费金额")
    private String realRefundAmount;

    @Schema(title = "退款请求时间")
    private String refundRequestDate;

    @Schema(title ="退还商户手续费")
    private String returnMerchantFee;

    @Schema(title = "退款银行订单号")
    private String bankRefundOrderNo;

    @Schema(title = "退款银行流水号")
    private String bankRefundOrderId;

    @Schema(title = "支付方式")
    private String paymentMethod;

    @Schema(title = "商户账户扣账金额")
    private String disAccountAmount;

    @Schema(title = "优惠券信息")
    private List<BankPromotionInfo> bankPromotionInfoDTOs;

    @Schema(title = "优惠券信息")
    private String channelPromotionInfo;

    @Schema(title = "原单订单管控状态")
    private String orgFundControlCsStatus;

    @Schema(title = "原单订单解冻完成时间")
    private String orgCsUnFrozenCompleteDate;

    @Schema(title = "退款扣账户明细")
    private String refundAccountDetail;
}