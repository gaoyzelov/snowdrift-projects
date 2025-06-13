package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AllinPayVo
 *
 * @author gaoye
 * @date 2025/05/22 11:43:30
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "响应数据")
public class AllinPayVo implements Serializable {

    @Schema(title = "返回码")
    private String retcode;

    @Schema(title = "返回码说明")
    private String retmsg;
}