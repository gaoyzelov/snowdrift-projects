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

    /**
     * 页码兜底值（与 {@link PageDTO} 默认值一致）
     */
    private static final long DEFAULT_PAGE_NUM = 1L;

    /**
     * 页大小兜底值（与 {@link PageDTO} 默认值一致）
     */
    private static final long DEFAULT_PAGE_SIZE = 10L;

    private PageUtil() {
    }

    /**
     * 把 {@link PageDTO} 转换为 MyBatis-Plus {@link Page}。
     * <p>{@code pageDTO} 为 {@code null} 或其 {@code pageNum}/{@code pageSize} 为 {@code null} 时，
     * 分别兜底为默认页码 {@value #DEFAULT_PAGE_NUM} 与默认页大小 {@value #DEFAULT_PAGE_SIZE}，
     * 避免自动拆箱触发 {@link NullPointerException}。</p>
     */
    public static <T> Page<T> of(PageDTO pageDTO) {
        long pageNum = (pageDTO == null || pageDTO.getPageNum() == null) ? DEFAULT_PAGE_NUM : pageDTO.getPageNum();
        long pageSize = (pageDTO == null || pageDTO.getPageSize() == null) ? DEFAULT_PAGE_SIZE : pageDTO.getPageSize();
        return Page.of(pageNum, pageSize);
    }

    public static <T> PageData<T> toPageData(Page<T> page) {
        return PageData.<T>builder()
                .total(page.getTotal())
                .records(page.getRecords())
                .build();
    }
}
