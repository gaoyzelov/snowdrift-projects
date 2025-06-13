package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * StatementAccountDto
 *
 * @author gaoye
 * @date 2025/05/23 11:15:47
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "对账单参数")
public class StatementAccountDto extends AllinPayDto{

    @Schema(title = "交易日期,yyyymmdd", required = true)
    @NotBlank(message = "交易日期不能为空")
    private String date;

    @Schema(title = "随机字符串",required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    /**
     * 为空时传统对账单
     * 1：传统对账单
     * 2：数币对账单
     * 3 :商户个性化日对账单
     * 4 :商户个性化月对账单
     */
    @Schema(title = "对账文件类型",required = true)
    @NotBlank(message = "对账文件类型不能为空")
    private String filetype;

    @Schema(title = "签名方式",required = true)
    @NotBlank(message = "签名方式不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名",required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;

}