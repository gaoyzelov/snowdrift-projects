package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


/**
 * BalanceDto
 *
 * @author gaoye
 * @date 2025/05/23 10:02:13
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额查询")
public class BalanceDto extends AllinPayDto {

    @Schema(title = "版本号，目前只支持11", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "11";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "查询类型，目前支持01-账户余额查询", required = true)
    @NotBlank(message = "查询类型不能为空")
    private String accttype = "01";

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}