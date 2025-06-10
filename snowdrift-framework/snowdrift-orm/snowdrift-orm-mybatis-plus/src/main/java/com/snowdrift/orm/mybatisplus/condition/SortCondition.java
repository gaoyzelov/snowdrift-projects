package com.snowdrift.orm.mybatisplus.condition;

import io.swagger.annotations.Schema;
import io.swagger.annotations.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SortQuery
 *
 * @author gaoye
 * @date 2025/03/20 17:32:57
 * @description 排序条件
 * @since 1.0.0
 */
@Data
@Schema(value = "SortQuery", description = "排序条件")
public class SortCondition implements Serializable {

    @Schema(value = "排序字段", example = "userId",position = 98)
    private String sortBy;

    @Schema(value = "true-升序，false-降序", example = "false", position = 99)
    private Boolean asc = Boolean.TRUE;
}