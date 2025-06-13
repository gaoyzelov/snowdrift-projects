package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * UniPayStatusDto
 *
 * @author gaoye
 * @date 2025/05/23 17:24:27
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一查询参数")
public class UniPayStatusDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version  = "11";

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String reqsn;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "签名方式", required = true)
    @NotBlank(message = "签名方式不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}