package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;


/**
 * RefundDto
 *
 * @author gaoye
 * @date 2025/05/22 10:12:40
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "退款参数")
public class RefundDto extends AllinPayDto {

    @Schema(title = "退款金额，单位为分", required = true)
    @NotNull(message = "退款金额不能为空")
    private Long trxamt;

    @Schema(title = "商户退款订单号,最大长度50", required = true)
    @NotBlank(message = "商户退款订单号不能为空")
    @Length(max = 50, message = "商户退款订单号，最大长度50")
    private String reqsn;

    @Schema(title = "原交易订单号,最大长度50", required = true)
    @NotBlank(message = "原交易订单号不能为空")
    @Length(max = 50, message = "原交易订单号，最大长度50")
    private String oldreqsn;

    @Schema(title = "原交易流水号,最大长度50")
    private String oldtrxid;

    @Schema(title = "退款备注信息，最大长度300")
    @Length(max = 300, message = "退款备注信息，最大长度300")
    private String remark;

    @Schema(title = "随机字符串，最大长度32",required = true)
    @NotBlank(message = "随机字符串不能为空")
    @Length(max = 32, message = "随机字符串，最大长度32")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "交易结果通知地址，最大长度256")
    @Length(max = 256, message = "交易结果通知地址，最大长度256")
    private String notify_url;

    @Schema(title = "版本号，目前只支持12", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}