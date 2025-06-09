package com.snowdrift.core.result;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "分页数据")
public class PageData<T> implements Serializable {

    @Schema(description = "当前页", example = "1")
    private Long current;

    @Schema(description = "总页数", example = "1000")
    private Long pages;

    @Schema(description = "每页数据条数", example = "10")
    private Long size;

    @Schema(description = "总数", example = "99999")
    private Long total;

    @Schema(description = "数据列表")
    private List<T> records;
}