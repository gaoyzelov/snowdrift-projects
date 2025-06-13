package com.snowdrift.orm.mybatisplus.condition;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
@Schema(title = "PageQuery", description = "分页查询条件")
public class PageCondition {

    @Schema(title = "页码", required = true, example = "1")
    @NotNull(message = "页码不能为空")
    @Min(title = 1, message = "页码必须为大于0的整数")
    private Integer page;

    @Schema(title = "每页显示数据条数", required = true, example = "10")
    @NotNull(message = "每页显示数据条数不能为空")
    @Min(title = 1, message = "每页显示数据不能小于1条")
    @Max(title = 500, message = "每页显示数据不能超过500条")
    private Integer limit;

    @Schema(title = "排序条件")
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
    @Schema(title = "SortQuery", description = "排序条件")
    public static class SortCondition implements Serializable {

        @Schema(title = "排序字段", example = "userId", required = true)
        @NotBlank(message = "排序字段不能为空")
        private String sortBy;


        @Schema(title = "true-升序，false-降序", example = "false", required = true)
        @NotNull(message = "排序方式不能为空")
        private Boolean asc = Boolean.TRUE;
    }
}