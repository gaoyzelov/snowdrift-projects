package com.snowdrift.test.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "点坐标")
public class PointDto implements Serializable {

    @ApiModelProperty(value = "x坐标")
    private String x;

    @ApiModelProperty(value = "y坐标")
    private String y;
}