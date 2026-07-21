package com.snowdrift.framework.orm.mp.util;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snowdrift.framework.orm.core.PageDTO;
import com.snowdrift.framework.orm.core.PageData;

/**
 * PageUtil
 *
 * @author gaoyzelov
 * @description 分页工具类
 * @date 2026/7/21
 * @since 1.0.0
 */
public final class PageUtil {

    private PageUtil() {
    }

    public static <T> Page<T> of(PageDTO pageDTO) {
        return Page.of(pageDTO.getPageNum(),pageDTO.getPageSize());
    }

    public static <T> PageData<T> toPageData(Page<T> page) {
        return PageData.<T>builder()
                .total(page.getTotal())
                .records(page.getRecords())
                .build();
    }
}
