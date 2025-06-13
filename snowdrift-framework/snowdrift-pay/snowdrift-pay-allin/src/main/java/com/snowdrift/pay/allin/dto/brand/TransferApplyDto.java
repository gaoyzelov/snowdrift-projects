package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

/**
 * TransferApplyDto
 *
 * @author gaoye
 * @date 2025/05/29 14:21:27
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "转账汇款申请参数")
public class TransferApplyDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "外部订单号", required = true)
    @NotBlank(message = "外部订单号不能为空")
    private String bizOrderNo;

    @Schema(title = "金额，单位：分", required = true)
    @NotNull(message = "金额不能为空")
    private Long amount;

    @Schema(title = "银行卡号")
    private String bankCard;

    /**
     * 转账事由
     * 00 	品牌方分账
     * 01   代理商分账
     * 02   联营商分账
     * 03   经销商分账
     * 04   门店分账
     * 99   其他
     */
    @Schema(title = "转账事由,最大长度2", required = true)
    @NotBlank(message = "转账事由不能为空")
    @Length(max = 2, message = "转账事由,最大长度2")
    @Pattern(regexp = "^(00|01|02|03|04|99)$", message = "转账事由格式错误")
    private String paymentCause;

    @Schema(title = "电子回单附言,最大长度59", required = true)
    @NotBlank(message = "电子回单附言不能为空")
    @Length(max = 59, message = "电子回单附言,最大长度59")
    private String summary;

    @Schema(title = "备注,最大长度64", required = true)
    @NotBlank(message = "备注不能为空")
    @Length(max = 64, message = "备注,最大长度64")
    private String remark;

    @Schema(title = "客户端IP", required = true)
    @NotBlank(message = "客户端IP不能为空")
    private String consumerIp;

    @Schema(title = "转账汇款业务信息JSON")
    private String businessInfo;

    @Schema(title = "交易单据编号", required = true)
    @NotNull(message = "交易单据编号不能为空")
    private Long documentId;
}