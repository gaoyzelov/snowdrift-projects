package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverPhoneBindVo
 *
 * @author gaoye
 * @date 2025/06/05 09:10:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方短信认证触发结果")
public class ReceiverBindPhoneApplyVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "手机号码")
    private String phone;
}