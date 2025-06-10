package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * EnterprisePayInfo
 *
 * @author gaoye
 * @date 2025/06/06 11:40:34
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "渠道拓展信息")
public class EnterprisePayInfo implements Serializable {

    @Schema(title = "企业信息列表")
    private List<EnterpriseInfo> enterpriseInfoList;

    @Schema(title = "发票金额")
    private BigDecimal invoiceAmount;
}