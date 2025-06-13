package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * StatementAccountVo
 *
 * @author gaoye
 * @date 2025/05/23 11:20:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "获取对账单结果")
public class StatementAccountVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "对账单文件下载地址，2分钟有效期")
    private String url;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名")
    private String sign;
}