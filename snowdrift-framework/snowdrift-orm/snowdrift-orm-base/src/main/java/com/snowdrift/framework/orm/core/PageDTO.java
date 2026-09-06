package com.snowdrift.framework.orm.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * PageDTO
 *
 * @author gaoyzelov
 * @description 分页参数
 * @date 2026/7/21
 * @since 1.0.0
 */
@Data
@Schema(description = "分页参数")
public class PageDTO implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码")
    private Long pageNum = 1L;

    /**
     * 单页数据条数
     */
    @Schema(description = "单页数据条数")
    private Long pageSize = 10L;
}
