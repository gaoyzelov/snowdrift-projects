package com.snowdrift.core.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * PageData
 *
 * @author gaoye
 * @date 2025/03/20 18:57:34
 * @description 分页查询结果
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "PageData", description = "分页数据")
public class PageData<T> implements Serializable {

    @ApiModelProperty(value = "当前页", example = "1")
    private Long current;

    @ApiModelProperty(value = "总页数", example = "1000", position = 1)
    private Long pages;

    @ApiModelProperty(value = "每页数据条数", example = "10", position = 2)
    private Long size;

    @ApiModelProperty(value = "总数", example = "99999", position = 3)
    private Long total;

    @ApiModelProperty(value = "数据列表", position = 4)
    private List<T> records;
}