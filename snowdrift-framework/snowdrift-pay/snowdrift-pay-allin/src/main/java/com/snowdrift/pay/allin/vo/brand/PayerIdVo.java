package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * PayerIdVo
 *
 * @author gaoye
 * @date 2025/06/05 09:38:20
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "获取付款方id结果")
public class PayerIdVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "付款人ID")
    private String bizUserId;
}