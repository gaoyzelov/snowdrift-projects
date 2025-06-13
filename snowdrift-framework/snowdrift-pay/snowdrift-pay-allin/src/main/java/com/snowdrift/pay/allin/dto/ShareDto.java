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
 * ShareDto
 *
 * @author gaoye
 * @date 2025/05/23 13:04:08
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "分账参数")
public class ShareDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "分账流水号", required = true)
    @NotBlank(message = "分账流水号不能为空")
    private String reqsn;

    @Schema(title = "待分账的交易的通联交易单号", required = true)
    @NotBlank(message = "待分账的交易的通联交易单号不能为空")
    private String oldtrxid;

    @Schema(title = "入金商户号", required = true)
    @NotBlank(message = "入金商户号不能为空")
    private String payeecusid;

    @Schema(title = "入金商户流水号")
    private String payeereqsn;

    @Schema(title = "分账金额，单位为分", required = true)
    @NotNull(message = "分账金额不能为空")
    private Long trxamt;

    @Schema(title = "分账备注,最大长度100")
    @Length(max = 100, message = "分账备注长度不能超过100")
    private String remark;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}