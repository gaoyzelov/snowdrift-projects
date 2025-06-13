package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverBindCardApplyDto
 *
 * @author gaoye
 * @date 2025/06/10 10:04:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "收款方银行卡验证")
public class ReceiverBindCardApplyDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号",required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "银行卡类型,1-借记卡",required = true)
    @NotNull(message = "银行卡类型不能为空")
    private Long cardType = 1L;

    @Schema(title = "银行卡号",required = true)
    @NotBlank(message = "银行卡号不能为空")
    private String cardNo;

    @Schema(title = "银行预留手机",required = true)
    @NotBlank(message = "银行预留手机不能为空")
    private String phone;

    @Schema(title = "姓名",required = true)
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(title = "证件类型,仅支持身份证",required = true)
    @NotNull(message = "证件类型不能为空")
    private Long identityType = 1L;

    @Schema(title = "证件号码",required = true)
    @NotBlank(message = "证件号码不能为空")
    private String identityNo;
}