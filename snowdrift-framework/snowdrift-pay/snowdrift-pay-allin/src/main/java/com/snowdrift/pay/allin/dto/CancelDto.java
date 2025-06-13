package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


/**
 * CancelDto
 *
 * @author gaoye
 * @date 2025/05/22 18:59:35
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "交易撤销参数")
public class CancelDto extends AllinPayDto {

    @Schema(title = "商户撤销交易单号", required = true)
    @NotBlank(message = "商户撤销交易单号不能为空")
    private String reqsn;

    @Schema(title = "原订单金额，单位为分", required = true)
    @NotNull(message = "原订单金额不能为空")
    private Long trxamt;

    @Schema(title = "原交易的商户交易单号")
    private String oldreqsn;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}