package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverUnbindCardVo
 *
 * @author gaoye
 * @date 2025/06/10 10:12:18
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方银行卡解绑返回结果")
public class ReceiverUnbindCardVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;
}