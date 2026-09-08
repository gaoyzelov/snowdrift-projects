package com.snowdrift.framework.web.xss;

import org.apache.commons.lang3.StringUtils;

/**
 * 简单 XSS 清洗器，将 HTML 特殊字符转义为实体。
 * <p>
 * 零额外依赖，适合作为默认兜底；但它只是对 {@code & < > " '} 做 HTML 实体转义，
 * <strong>不适用于 JSON 输出</strong>——实体转义后的字符串写入 JSON 会改变载荷内容，
 * 且对已经转义过的输入再次清洗会产生双重编码。
 * </p>
 * <p>
 * 该默认 Bean 通过 {@code @ConditionalOnMissingBean} 注册
 * （见 {@link com.snowdrift.framework.web.config.SnowdriftWebConfiguration#xssCleaner()}），
 * 富文本或对输出格式有要求的场景请注入基于 Jsoup 等实现的自定义 {@link XssCleaner} 覆盖。
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
