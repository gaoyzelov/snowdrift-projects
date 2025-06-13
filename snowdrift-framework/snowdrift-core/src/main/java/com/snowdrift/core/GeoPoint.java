package com.snowdrift.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * GeoPoint
 *
 * @author gaoye
 * @date 2025/06/10 09:47:53
 * @description 经纬度坐标点
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode
@Schema(title = "GeoPoint", description = "GPS坐标")
public class GeoPoint implements Serializable {

    /**
     * 经度
     */
    @Schema(title = "经度", example = "113.000001")
    private Double longitude;

    /**
     * 纬度
     */
    @Schema(title = "纬度", example = "23.000001")
    private Double latitude;

}