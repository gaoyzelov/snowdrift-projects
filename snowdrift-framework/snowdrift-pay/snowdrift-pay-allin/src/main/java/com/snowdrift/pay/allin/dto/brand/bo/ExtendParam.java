package com.snowdrift.pay.allin.dto.brand.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ExtendParam
 *
 * @author gaoye
 * @date 2025/06/11 16:30:59
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "扩展参数")
public class ExtendParam implements Serializable {

    @Schema(title = "字段名称")
    private String fieldName;

    @Schema(title = "是否必填")
    private Boolean required;
}