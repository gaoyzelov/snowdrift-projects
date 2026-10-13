package com.snowdrift.framework.web.wrapper;

import com.snowdrift.framework.web.xss.XssCleaner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * XSS 防护请求包装器 — 按开关对用户可控输入做 XSS 清洗。
 * <p>
 * 清洗逻辑委托给可插拔的 {@link XssCleaner} 接口实现，默认使用
 * {@link com.snowdrift.framework.web.xss.SimpleXssCleaner}（HTML 实体转义），富文本场景可注入 Jsoup 等实现覆盖。
 * </p>
 * <p>
 * <b>输入侧清洗默认全部关闭</b>（参数、Header、QueryString 三类互不影响，各由自己的开关控制）。
 * 原因是输入侧转义会把合法输入改写成 HTML 实体，从而篡改业务数据本身：
 * </p>
 * <ul>
 *   <li>请求参数：{@code a&b} → {@code a&amp;b}，污染审计与日志里记录的入参，也可能破坏签名校验、令牌解析</li>
 *   <li>Header：{@code If-None-Match: "v1"} → {@code &quot;v1&quot;}（条件请求失效）、
 *       {@code Authorization: Bearer a.b&c} → {@code Bearer a.b&amp;c}（令牌被改写，认证失败）</li>
 *   <li>QueryString：下游对原始查询串做签名校验时，转义后签名必然不匹配</li>
 * </ul>
 * <p>
 * XSS 属<b>输出上下文</b>问题，应在渲染/响应阶段按上下文编码；本模块的
 * {@link com.snowdrift.framework.web.xss.XssJsonBodyAdvice} 与默认清洗器的 Javadoc 也是这个立场。
 * 保留开关是为了让依赖旧行为的下游平滑迁移，新接入方建议保持关闭。
 * </p>
 * <p>
 * <b>口径一致性</b>：{@link #getHeader(String)} 与 {@link #getHeaders(String)} 结果一致（同一个头不会出现两个不同的值）；
 * {@link #getHeaderNames()} 始终不转义 —— Header 名是查值的键，转义后无法再用于 {@code getHeader(name)}。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final XssCleaner xssCleaner;

    /** 是否清洗请求参数（含 {@link #getQueryString()}）；默认关闭，见 {@code XssProperties#escapeParameters} */
    private final boolean escapeParameters;

    /** 是否清洗请求 Header；默认关闭，见 {@code XssProperties#escapeHeaders} */
    private final boolean escapeHeaders;

    /**
     * 等价于 {@code XssRequestWrapper(request, xssCleaner, false, false)}：<b>不清洗任何输入</b>。
     */
    public XssRequestWrapper(HttpServletRequest request, XssCleaner xssCleaner) {
        this(request, xssCleaner, false, false);
    }

    /**
     * 等价于 {@code XssRequestWrapper(request, xssCleaner, escapeParameters, false)}：只控制参数清洗。
     */
    public XssRequestWrapper(HttpServletRequest request, XssCleaner xssCleaner, boolean escapeParameters) {
        this(request, xssCleaner, escapeParameters, false);
    }

    /**
     * @param request          原始请求
     * @param xssCleaner       清洗器
     * @param escapeParameters 是否清洗请求参数与 QueryString。会改写业务数据（{@code a&b} → {@code a&amp;b}），故默认 {@code false}
     * @param escapeHeaders    是否清洗请求 Header。会破坏协议语义值（{@code If-None-Match} 的引号、令牌中的 {@code &}），故默认 {@code false}
     */
    public XssRequestWrapper(HttpServletRequest request, XssCleaner xssCleaner,
                             boolean escapeParameters, boolean escapeHeaders) {
        super(request);
        this.xssCleaner = xssCleaner;
        this.escapeParameters = escapeParameters;
        this.escapeHeaders = escapeHeaders;
    }

    // ========== 参数过滤 ==========

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return escapeParameters ? xssCleaner.clean(value) : value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null || !escapeParameters) {
            return values;
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
        if (!escapeParameters) {
            return Collections.unmodifiableMap(map);
        }
        return Collections.unmodifiableMap(map.entrySet().stream()
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
                )));
    }

    /**
     * 对查询字符串做 XSS 转义（{@code escapeParameters} 开启时）
     */
    @Override
    public String getQueryString() {
        return escapeParameters ? xssCleaner.clean(super.getQueryString()) : super.getQueryString();
    }

    // ========== Header 过滤 ==========

    /**
     * 对请求头值做 XSS 转义（{@code escapeHeaders} 开启时）
     */
    @Override
    public String getHeader(String name) {
        return cleanHeader(super.getHeader(name));
    }

    /**
     * 与 {@link #getHeader(String)} 保持同一口径：同一个头经两个访问器必须得到相同的值
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        Enumeration<String> headers = super.getHeaders(name);
        if (headers == null || !escapeHeaders) {
            return headers;
        }
        ArrayList<String> cleaned = new ArrayList<>();
        while (headers.hasMoreElements()) {
            cleaned.add(cleanHeader(headers.nextElement()));
        }
        return Collections.enumeration(cleaned);
    }

    private String cleanHeader(String value) {
        return escapeHeaders ? xssCleaner.clean(value) : value;
    }
}
