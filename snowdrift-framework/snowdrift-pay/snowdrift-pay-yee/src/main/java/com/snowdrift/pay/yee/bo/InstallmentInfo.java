package com.snowdrift.pay.yee.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * installmentInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:17:50
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "分期信息")
public class InstallmentInfo implements Serializable {

    @Schema(title = "分期期数")
    private String instNumber;
}