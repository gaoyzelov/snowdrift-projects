package com.snowdrift.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * PointDto
 *
 * @author gaoye
 * @date 2025/03/24 19:34:10
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "点坐标")
public class PointDto implements Serializable {

    @Schema(description = "x坐标")
    private String x;

    @Schema(description = "y坐标")
    private String y;
}