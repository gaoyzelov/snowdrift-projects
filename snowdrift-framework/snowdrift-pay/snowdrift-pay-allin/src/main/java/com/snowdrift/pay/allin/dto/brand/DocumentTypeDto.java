package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * DocumentTypeDto
 *
 * @author gaoye
 * @date 2025/06/11 16:18:59
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "单据类型查询参数")
public class DocumentTypeDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "单据类型")
    private String documentType;
}