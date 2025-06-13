package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * ShareStatusDto
 *
 * @author gaoye
 * @date 2025/05/23 13:23:44
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "分账交易查询参数")
public class ShareStatusDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "分账流水号", required = true)
    @NotBlank(message = "分账流水号不能为空")
    private String reqsn;

    @Schema(title = "出金交易单号", required = true)
    @NotBlank(message = "出金交易单号不能为空")
    private String payertrxid;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}