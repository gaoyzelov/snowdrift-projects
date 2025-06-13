package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AllinBrandVo
 *
 * @author gaoye
 * @date 2025/05/29 14:00:33
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "智品牌接口通用返回结果")
public class AllinBrandVo implements Serializable {

    @Schema(title = "请求返回码")
    private String code;

    @Schema(title = "请求返回码描述")
    private String msg;

    @Schema(title = "业务返回码")
    private String bizCode;

    @Schema(title = "业务返回码描述")
    private String bizMsg;

    @Schema(title = "签名")
    private String sign;
}