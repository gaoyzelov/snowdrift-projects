package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * DocumentSyncDto
 *
 * @author gaoye
 * @date 2025/06/11 16:18:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "单据同步参数")
public class DocumentSyncDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "业务单据编号", required = true)
    @NotBlank(message = "业务单据编号不能为空")
    private String extDocumentNo;

    @Schema(title = "金额,单位：分", required = true)
    @NotNull(message = "金额不能为空")
    private Long amount;

    @Schema(title = "业务单据类型", required = true)
    @NotBlank(message = "业务单据类型不能为空")
    private String documentType;

    @Schema(title = "日期区间,yyyyMM-yyyyMM", required = true)
    @NotBlank(message = "日期区间不能为空")
    private String dateSection;

    @Schema(title = "事由", required = true)
    @NotBlank(message = "事由不能为空")
    private String subjectMatter;

    @Schema(title = "收款对象信息", required = true)
    @NotBlank(message = "收款对象信息不能为空")
    private String object;

    @Schema(title = "业务数据", required = true)
    @NotBlank(message = "业务数据不能为空")
    private String businessInfo;

    @Schema(title = "操作类型,save-保存 edit-编辑", required = true)
    @NotBlank(message = "操作类型不能为空")
    private String operationType;
}