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
 * ShareRevokeDto
 *
 * @author gaoye
 * @date 2025/05/23 13:14:58
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "分账回退参数")
public class ShareRevokeDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "出金交易单号", required = true)
    @NotBlank(message = "出金交易单号不能为空")
    private String payertrxid;

    @Schema(title = "回退流水号", required = true)
    @NotBlank(message = "回退流水号不能为空")
    private String reqsn;

    @Schema(title = "出金方回退流水号")
    private String payerreqsn;

    @Schema(title = "回退金额，单位为分", required = true)
    @NotNull(message = "回退金额不能为空")
    private Long trxamt;

    @Schema(title = "备注，最大长度100", required = true)
    @Length(max = 100, message = "备注长度不能超过100")
    @NotBlank(message = "备注不能为空")
    private String remark;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}