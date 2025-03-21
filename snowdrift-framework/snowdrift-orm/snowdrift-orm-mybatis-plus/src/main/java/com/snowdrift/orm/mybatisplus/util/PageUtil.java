package com.snowdrift.orm.mybatisplus.util;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.CaseFormat;
import com.snowdrift.core.result.PageData;
import com.snowdrift.orm.mybatisplus.condition.PageCondition;
import org.apache.commons.collections4.CollectionUtils;

/**
 * PageUtil
 *
 * @author gaoye
 * @date 2025/03/20 18:34:09
 * @description 分页条件处理工具类
 * @since 1.0.0
 */
public class PageUtil {

    /**
     * 分页条件构建
     *
     * @param pageCondition 分页查询参数
     * @return Page<E>
     */
    public static <E> Page<E> pageOf(PageCondition pageCondition) {
        Page<E> page = Page.of(pageCondition.getPage(), pageCondition.getLimit());
        if (CollectionUtils.isEmpty(pageCondition.getSorts())) {
            return page;
        }
        for (PageCondition.SortCondition sortCondition : pageCondition.getSorts()) {
            String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortCondition.getSortBy());
            OrderItem orderItem = sortCondition.getAsc() ? OrderItem.asc(column) : OrderItem.desc(column);
            page.addOrder(orderItem);
        }
        return page;
    }

    /**
     * 分页结果封装
     *
     * @param page 分页结
     * @return PageData
     */
    public static <T> PageData<T> wrap(Page<T> page) {
        return new PageData<T>()
                .setCurrent(page.getCurrent())
                .setPages(page.getPages())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(page.getRecords());
    }

}