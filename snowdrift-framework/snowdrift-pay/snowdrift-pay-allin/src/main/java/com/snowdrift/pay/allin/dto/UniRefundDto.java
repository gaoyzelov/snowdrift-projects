package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * UniRefundDto
 *
 * @author gaoye
 * @date 2025/05/23 16:58:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一退款参数")
public class UniRefundDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    private String version = "11";

    @Schema(title = "退款金额", required = true)
    @NotNull(message = "退款金额不能为空")
    private Long trxamt;

    @Schema(title = "商户退款单号", required = true)
    @NotBlank(message = "商户退款单号不能为空")
    private String reqsn;

    @Schema(title = "原交易订单号", required = true)
    @NotBlank(message = "原交易订单号不能为空")
    private String oldreqsn;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "交易结果通知地址")
    private String notify_url;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}