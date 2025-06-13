package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

/**
 * BalanceSettlementDto
 *
 * @author gaoye
 * @date 2025/05/23 10:18:20
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额结算参数")
public class BalanceSettlementDto extends AllinPayDto {

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "11";

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "结算金额", required = true)
    @NotNull(message = "结算金额不能为空")
    private Long trxamt;

    @Schema(title = "请求流水号", required = true)
    @NotBlank(message = "请求流水号不能为空")
    private String reqsn;

    @Schema(title = "结算类型，1-快速结算，2-标准结算", required = true)
    @NotNull(message = "结算类型不能为空")
    @Range(min = 1, max = 2, message = "结算类型范围1-快速结算，2-标准结算")
    private Integer deptype;

    @Schema(title = "结算备注，最大长度100")
    @Length(max = 100, message = "结算备注最大长度100")
    private String remark;

    @Schema(title = "银行摘要，最大长度64")
    @Length(max = 64, message = "银行摘要最大长度64")
    private String summary;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;
}