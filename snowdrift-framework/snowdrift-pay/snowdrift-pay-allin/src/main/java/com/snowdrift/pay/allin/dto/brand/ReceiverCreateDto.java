package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

/**
 * ReceiverCreateDto
 *
 * @author gaoye
 * @date 2025/06/03 16:28:10
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款方创建参数")
public class ReceiverCreateDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "外部系统收款方编号", required = true)
    @NotBlank(message = "外部系统收款方编号不能为空")
    private String customerUserId;

    @Schema(title = "收款方类型，2-企业，3-个人", required = true)
    @NotNull(message = "收款方类型不能为空")
    @Range(min = 2, max = 3, message = "收款方类型值有误，仅支持2或3")
    private Long memberType;

    @Schema(title = "收款方角色，00-门店，02-经销商，03-联营商，04-代理商，99-其他", required = true)
    @NotBlank(message = "收款方角色不能为空")
    @Pattern(regexp = "^(00|02|03|04|99)$", message = "收款方角色错误")
    private String memberRole;

    @Schema(title = "业务名称，最大长度64")
    @Length(max = 64, message = "业务名称,最大长度64")
    private String businessName;
}