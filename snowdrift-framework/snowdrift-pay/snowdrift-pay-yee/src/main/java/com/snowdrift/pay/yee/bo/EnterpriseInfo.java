package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * EnterpriseInfo
 *
 * @author gaoye
 * @date 2025/06/06 11:41:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "企业信息")
public class EnterpriseInfo implements Serializable {

    @Schema(title = "企业账户id")
    private String enterpriseId;

    @Schema(title = "企业支付金额")
    private BigDecimal enterprisePayAmount;
}