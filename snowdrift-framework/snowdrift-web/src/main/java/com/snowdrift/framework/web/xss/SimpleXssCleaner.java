package com.snowdrift.framework.web.xss;

import org.apache.commons.lang3.StringUtils;

/**
 * 简单 XSS 清洗器，将 HTML 特殊字符转义为实体。
 * <p>
 * 零额外依赖，适用于纯 JSON API 场景。
 * 富文本场景请注入基于 Jsoup Safelist 的实现覆盖此 Bean。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class SimpleXssCleaner implements XssCleaner {

    private static final String[] SEARCH = {"&", "<", ">", "\"", "'"};
    private static final String[] REPLACE = {"&amp;", "&lt;", "&gt;", "&quot;", "&#39;"};

    @Override
    public String clean(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return StringUtils.replaceEach(value, SEARCH, REPLACE);
    }
}
