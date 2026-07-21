package com.snowdrift.framework.orm.core;

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
public class PageData<T> implements Serializable {

    /**
     * 总数
     */
    private Long total;

    /**
     * 数据记录列表
     */
    private List<T> records;
}
