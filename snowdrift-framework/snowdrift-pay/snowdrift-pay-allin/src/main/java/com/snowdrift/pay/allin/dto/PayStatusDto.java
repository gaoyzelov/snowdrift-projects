package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * PayStatusDto
 *
 * @author gaoye
 * @date 2025/05/22 10:43:08
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "交易状态查询")
public class PayStatusDto extends AllinPayDto {

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String reqsn;

    @Schema(title = "平台交易流水")
    private String trxid;

    @Schema(title = "版本号，目前只支持12", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "随机字符串，最大长度32", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}