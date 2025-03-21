package com.snowdrift.orm.mybatisplus.condition;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * PageQuery
 *
 * @author gaoye
 * @date 2025/03/20 17:31:53
 * @description 分页查询参数
 * @since 1.0.0
 */
@Getter
@Setter
@ApiModel(value = "PageQuery", description = "分页查询条件")
public class PageCondition {

    @ApiModelProperty(value = "页码", required = true, example = "1", position = 97)
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须为大于0的整数")
    private Integer page;

    @ApiModelProperty(value = "每页显示数据条数", required = true, example = "10", position = 98)
    @NotNull(message = "每页显示数据条数不能为空")
    @Min(value = 1, message = "每页显示数据不能小于1条")
    @Max(value = 500, message = "每页显示数据不能超过500条")
    private Integer limit;

    @ApiModelProperty(value = "排序条件", position = 99)
    @Valid
    private List<SortCondition> sorts;

    /**
     * SortQuery
     *
     * @author gaoye
     * @date 2025/03/20 17:32:57
     * @description 排序条件
     * @since 1.0.0
     */
    @Data
    @ApiModel(value = "SortQuery", description = "排序条件")
    public static class SortCondition implements Serializable {

        @ApiModelProperty(value = "排序字段", example = "userId", required = true)
        @NotBlank(message = "排序字段不能为空")
        private String sortBy;


        @ApiModelProperty(value = "true-升序，false-降序", example = "false", required = true, position = 1)
        @NotNull(message = "排序方式不能为空")
        private Boolean asc = Boolean.TRUE;
    }
}