package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * CloseVo
 *
 * @author gaoye
 * @date 2025/05/22 19:15:11
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "交易关闭响应数据")
public class CloseVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "交易状态")
    private String trxstatus;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "错误原因")
    private String errmsg;

    @Schema(title = "签名")
    private String sign;
}