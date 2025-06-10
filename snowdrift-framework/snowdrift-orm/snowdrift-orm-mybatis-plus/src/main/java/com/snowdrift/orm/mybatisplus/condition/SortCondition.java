package com.snowdrift.orm.mybatisplus.condition;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "SortQuery", description = "排序条件")
public class SortCondition implements Serializable {

    @Schema(title = "排序字段", example = "userId")
    private String sortBy;

    @Schema(title = "true-升序，false-降序", example = "false")
    private Boolean asc = Boolean.TRUE;
}