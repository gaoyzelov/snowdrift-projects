package com.snowdrift.framework.web.wrapper;

import com.snowdrift.framework.web.xss.XssCleaner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * XSS 防护请求包装器 — 对请求参数、请求头、请求属性等做 XSS 清洗。
 * <p>
 * 清洗逻辑委托给可插拔的 {@link XssCleaner} 接口实现。
 * 默认使用 {@link com.snowdrift.framework.web.xss.SimpleXssCleaner}（HTML 实体转义），
 * 富文本场景可注入 Jsoup 等实现覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final XssCleaner xssCleaner;

    public XssRequestWrapper(HttpServletRequest request, XssCleaner xssCleaner) {
        super(request);
        this.xssCleaner = xssCleaner;
    }

    // ========== 参数过滤 ==========

    @Override
    public String getParameter(String name) {
        return xssCleaner.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = xssCleaner.clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = super.getParameterMap();
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            String[] values = e.getValue();
                            String[] cleaned = new String[values.length];
                            for (int i = 0; i < values.length; i++) {
                                cleaned[i] = xssCleaner.clean(values[i]);
                            }
                            return cleaned;
                        }
                ));
    }

    /**
     * 对请求属性值做 XSS 转义
     */
    @Override
    public Object getAttribute(String name) {
        Object value = super.getAttribute(name);
        if (value instanceof String str) {
            return xssCleaner.clean(str);
        }
        return value;
    }

    /**
     * 对请求头值做 XSS 转义
     */
    @Override
    public String getHeader(String name) {
        return xssCleaner.clean(super.getHeader(name));
    }

    /**
     * 对查询字符串做 XSS 转义
     */
    @Override
    public String getQueryString() {
        return xssCleaner.clean(super.getQueryString());
    }
}
