package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ClearNotifyVo
 *
 * @author gaoye
 * @date 2025/06/06 13:14:52
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "清算结果通知")
public class ClearNotifyVo implements INotifyVo {

    @Schema(title = "下单请求收款的金额")
    private String orderAmount;

    @Schema(title = "商户收款订单号")
    private String orderId;

    @Schema(title = "发起方商户编号")
    private String parentMerchantNo;

    @Schema(title = "商户收款请求号对应在易宝的收款订单号")
    private String uniqueOrderNo;

    @Schema(title = "收款商户编号")
    private String merchantNo;

    @Schema(title = "订单状态,SUCCESS（订单清算成功）")
    private String status;

    @Schema(title = "清算完成时间。格式：yyyy-MM-dd HH:mm:ss")
    private String csSuccessDate;

    @Schema(title = "商户手续费")
    private String merchantFee;

    @Schema(title = "入账金额")
    private String ypSettleAmount;

    @Schema(title = "手续费承担方商编")
    private String feeMerchantNo;

    @Schema(title = "手续费收取方式")
    private String feeType;

    @Schema(title = "剩余可分账金额")
    private String unSplitAmount;

    @Schema(title = "订单管控状态")
    private String fundControlCsStatus;

    @Schema(title = "管控订单解冻时间")
    private String csUnFrozenCompleteDate;

    @Schema(title = "原始可分账金额")
    private BigDecimal originalDivideAmount;
}