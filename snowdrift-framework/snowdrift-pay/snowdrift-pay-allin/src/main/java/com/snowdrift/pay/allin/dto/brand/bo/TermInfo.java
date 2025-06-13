package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * TermInfo
 *
 * @author gaoye
 * @date 2025/06/05 10:17:24
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "终端信息")
public class TermInfo implements Serializable {

    @Schema(title = "终端号", required = true)
    @NotBlank(message = "终端号不能为空")
    private String termId;

    /**
     * 受理终端设备实时经纬度信息，格式为纬度/经度，+表示北纬、东经，-表示南纬、西经。
     * 如：+37.12/-121.213。
     */
    @Schema(title = "经纬度")
    private String location;

    @Schema(title = "终端设备IP")
    private String deviceIp;
}