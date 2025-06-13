package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverBalanceVo
 *
 * @author gaoye
 * @date 2025/06/05 09:28:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方余额查询结果")
public class ReceiverBalanceVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "总额")
    private Long allAmount;

    @Schema(title = "冻结额")
    private String freezeAmount;
}