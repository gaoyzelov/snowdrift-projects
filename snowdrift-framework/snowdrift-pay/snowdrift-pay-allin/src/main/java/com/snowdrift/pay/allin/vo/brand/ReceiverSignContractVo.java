package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverSignContractVo
 *
 * @author gaoye
 * @date 2025/06/10 10:35:09
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方电子协议签约结果")
public class ReceiverSignContractVo extends AllinBrandVo{

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "电子协议签约地址")
    private String signUrl;
}