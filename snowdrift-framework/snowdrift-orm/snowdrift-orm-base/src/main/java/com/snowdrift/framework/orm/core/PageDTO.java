package com.snowdrift.framework.orm.core;

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
public class PageDTO implements Serializable {

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 单页数据条数
     */
    private Long pageSize = 10L;
}
