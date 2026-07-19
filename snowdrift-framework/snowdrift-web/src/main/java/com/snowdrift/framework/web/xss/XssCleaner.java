package com.snowdrift.framework.web.xss;

/**
 * XSS 清洗器，可插拔实现。
 * <p>
 * 默认使用 {@link SimpleXssCleaner} 做 HTML 实体转义（零额外依赖），
 * 富文本场景可注入基于 Jsoup 等第三方库的实现覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
@FunctionalInterface
public interface XssCleaner {

    /**
     * 对输入字符串做 XSS 清洗
     *
     * @param value 原始值，可能为 null
     * @return 清洗后的值
     */
    String clean(String value);
}
