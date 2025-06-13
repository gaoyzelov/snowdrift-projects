package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * SettlementDocumentDto
 *
 * @author gaoye
 * @date 2025/05/23 11:04:27
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "获取结算单参数")
public class SettlementDocumentDto extends AllinPayDto {

    @Schema(title = "结算日期,yyyymmdd", required = true)
    @NotBlank(message = "结算日期不能为空")
    private String settdate;

    @Schema(title = "随机字符串", required = true)
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}