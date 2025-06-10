package com.snowdrift.pay.yee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WithdrawCardBindVo
 *
 * @author gaoye
 * @date 2025/06/06 15:24:36
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现卡绑定返回参数")
public class WithdrawCardBindVo extends ResponseVo {

    @Schema(title = "绑卡ID")
    private String bindId;
}