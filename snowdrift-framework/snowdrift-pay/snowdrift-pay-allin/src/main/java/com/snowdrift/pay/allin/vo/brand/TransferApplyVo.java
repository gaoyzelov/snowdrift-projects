package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * TransferApplyVo
 *
 * @author gaoye
 * @date 2025/05/29 14:21:42
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "转账汇款申请返回结果")
public class TransferApplyVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "通联转账单号")
    private String documentNo;

    @Schema(title = "转账状态")
    private String status;
}