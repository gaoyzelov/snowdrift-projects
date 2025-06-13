package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverCompanyInfoSetVo
 *
 * @author gaoye
 * @date 2025/06/03 16:50:00
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方公司信息设置返回结果")
public class ReceiverCompanyInfoSetVo extends AllinBrandVo{

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;
}