package com.snowdrift.pay.allin.vo.brand;

import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * DocumentTypeVo
 *
 * @author gaoye
 * @date 2025/06/11 16:27:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "单据类型返回结果")
public class DocumentTypeVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "单据类型列表,JsonArray字符串")
    private String documentTypes;

    @Schema(title = "总数")
    private Integer totalCount;

    public List<DocumentType> getDocumentTypeList() {
        if (StringUtils.isBlank(documentTypes) || StringUtils.equals(documentTypes, "[]")) {
            return null;
        }
        try {
            return JSON.parseArray(documentTypes, DocumentType.class);
        } catch (Exception e) {
            return null;
        }
    }
}