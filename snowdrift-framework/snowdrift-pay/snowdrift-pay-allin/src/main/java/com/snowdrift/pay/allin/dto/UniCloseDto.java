package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * UniCloseDto
 *
 * @author gaoye
 * @date 2025/05/23 17:41:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一关闭参数")
public class UniCloseDto extends AllinPayDto {

    @Schema(title = "版本号，目前只支持12", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "原商户订单号", required = true)
    @NotBlank(message = "原商户订单号不能为空")
    private String oldreqsn;

    @Schema(title = "是否音箱播报，0-不播报，1-播报")
    private Integer isiot;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}