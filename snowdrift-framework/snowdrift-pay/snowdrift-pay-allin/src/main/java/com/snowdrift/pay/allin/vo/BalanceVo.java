package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * BalanceVo
 *
 * @author gaoye
 * @date 2025/05/23 10:05:55
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额查询结果")
public class BalanceVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "账户金额，单位分")
    private Long amount;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名")
    private String sign;
}