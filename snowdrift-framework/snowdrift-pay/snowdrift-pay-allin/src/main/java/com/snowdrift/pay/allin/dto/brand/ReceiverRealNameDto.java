package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverRealNameDto
 *
 * @author gaoye
 * @date 2025/06/10 09:58:14
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款人实名认证参数")
public class ReceiverRealNameDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "姓名",required = true)
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(title = "身份证号码",required = true)
    @NotBlank(message = "身份证号码不能为空")
    private String identityNo;
}