package com.snowdrift.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "用户参数", description = "用户信息")
public class UserDto implements Serializable {

    @Schema(title = "用户ID", description = "自增ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1", defaultValue = "1", allowableValues = {"1", "2", "3"})
    private Integer id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "生日")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "坐标")
    private PointDto point;

    @Schema(description = "坐标集合")
    private List<PointDto> points;
}