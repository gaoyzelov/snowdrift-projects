package com.snowdrift.pay.allin.vo.brand;


import com.snowdrift.pay.allin.dto.brand.bo.RefundDetail;
import com.snowdrift.pay.allin.dto.brand.bo.SplitRefundDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * PayRefundDto
 *
 * @author gaoye
 * @date 2025/06/05 10:56:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "退款申请参数")
public class PayRefundDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "商户退款订单号", required = true)
    @NotBlank(message = "商户退款订单号不能为空")
    private String bizOrderNo;

    @Schema(title = "原商户订单号", required = true)
    @NotBlank(message = "原商户订单号不能为空")
    private String oldBizOrderNo;

    @Schema(title = "退款金额", required = true)
    @NotNull(message = "退款金额不能为空")
    private Long amount;

    @Schema(title = "平台手续费")
    private Long fee;

    @Schema(title = "商户备注",required = true)
    @NotBlank(message = "商户备注不能为空")
    private String remark;

    @Schema(title = "调拨模式,0-查询收银宝余额后调拨,1-直接调拨跳过查询收银宝余额")
    private String allocationModel;

    @Schema(title = "优惠信息")
    private String benefitDetail;

    @Schema(title = "原收银宝订单号")
    private String oldSybChannelOrderNo;

    @Schema(title = "退款列表")
    private List<RefundDetail> refundList;

    @Schema(title = "分账退款列表，已上送分账退款列表时，退款列表不生效")
    private List<SplitRefundDetail> splitRefundList;
}