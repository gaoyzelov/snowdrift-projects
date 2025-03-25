package com.snowdrift.test.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * TestDto
 *
 * @author gaoye
 * @date 2025/03/24 17:01:23
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@ApiModel
public class UserDto implements Serializable {

    @ApiModelProperty(value = "主键")
    private Integer id;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "生日")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @ApiModelProperty(value = "坐标")
    private PointDto point;

    @ApiModelProperty(value = "坐标集合")
    private List<PointDto> points;
}