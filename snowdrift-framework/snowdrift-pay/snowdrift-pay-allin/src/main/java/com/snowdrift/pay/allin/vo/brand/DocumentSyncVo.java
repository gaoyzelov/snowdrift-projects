package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * DocumentSyncVo
 *
 * @author gaoye
 * @date 2025/06/11 16:26:13
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "单据同步返回结果")
public class DocumentSyncVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "业务单据编号")
    private String extDocumentNo;

    @Schema(title = "业务单据编号ID")
    private Long documentId;
}