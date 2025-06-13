package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DocumentType
 *
 * @author gaoye
 * @date 2025/06/11 16:30:10
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "单据类型")
public class DocumentType implements Serializable {

    @Schema(title = "单据类型")
    private String documentType;

    @Schema(title = "单据类型描述")
    private String documentTypeDesc;

    @Schema(title = "扩展字段列表")
    private List<ExtendParam> extendParams;
}