package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverCreate
 *
 * @author gaoye
 * @date 2025/06/03 16:37:30
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方创建结果")
public class ReceiverCreateVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "外部系统收款方编号")
    private String customerUserId;

    @Schema(title = "收款方类型，1-待审核，2-审核成功，3-审核失败")
    private Long status;
}