package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * BalanceSettlementQueryDto
 *
 * @author gaoye
 * @date 2025/05/23 10:44:58
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额结算查询")
public class BalanceSettlementQueryDto extends AllinPayDto {

    @Schema(title = "版本号，目前只支持11", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "11";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "通联单号", required = true)
    @NotBlank(message = "通联单号不能为空")
    private String trxid;

    @Schema(title = "请求流水号", required = true)
    @NotBlank(message = "请求流水号不能为空")
    private String reqsn;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}