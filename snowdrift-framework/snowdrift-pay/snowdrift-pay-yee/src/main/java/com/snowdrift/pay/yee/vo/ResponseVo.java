package com.snowdrift.pay.yee.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ResponseVo
 *
 * @author gaoye
 * @date 2025/06/06 09:32:18
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "响应结果")
public class ResponseVo implements Serializable {

    @Schema(title = "接口返回码")
    @JsonAlias({"code","returnCode"})
    private String code;

    @Schema(title = "返回信息")
    @JsonAlias({"message","returnMsg"})
    private String message;
}