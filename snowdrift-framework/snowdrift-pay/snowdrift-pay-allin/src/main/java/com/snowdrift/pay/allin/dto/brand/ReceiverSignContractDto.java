package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ReceiverSignContractDto
 *
 * @author gaoye
 * @date 2025/06/10 10:31:23
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款方电子协议签约")
public class ReceiverSignContractDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "签订之后，跳转返回的页面地址", required = true)
    @NotBlank(message = "跳转返回的页面地址不能为空")
    private String jumpUrl;

    @Schema(title = "跳转页面类型,1-页面，2-小程序页面")
    private Long jumpPageType = 1L;

    @Schema(title = "访问终端类型，1-Mobile,2-PC", required = true)
    private Long source;

    /**
     * 个人收款方：该字段无需上送
     * 企业收款方：法人对私卡提现，需上送“法人姓名”；企业对公户提现，无需上送
     */
    @Schema(title = "法人签约名称")
    private String legalSignAcctName;
}