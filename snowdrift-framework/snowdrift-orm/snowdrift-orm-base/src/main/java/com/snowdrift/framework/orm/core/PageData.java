package com.snowdrift.framework.orm.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * PageData
 *
 * @author gaoyzelov
 * @description 分页数据
 * @date 2026/7/21
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "分页数据")
public class PageData<T> implements Serializable {

    /**
     * 总数
     */
    @Schema(description = "总数")
    private Long total;

    /**
     * 数据记录列表
     */
    @Schema(description = "数据记录列表")
    private List<T> records;
}
