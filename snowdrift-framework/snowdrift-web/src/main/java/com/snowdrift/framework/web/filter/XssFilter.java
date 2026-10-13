package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.web.properties.XssProperties;
import com.snowdrift.framework.web.wrapper.XssRequestWrapper;
import com.snowdrift.framework.web.xss.XssCleaner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * XSS 防护过滤器 — 按开关对请求参数 / Header / QueryString 做 XSS 清洗（<b>默认全部不清洗</b>，见下）。
 * <p>
 * 通过 {@code snowdrift.xss.enabled=true} 启用（默认关闭），
 * {@code exclude-path-patterns} 排除指定路径。
 * </p>
 * <p>
 * 输入侧清洗需额外显式开启，且参数与 Header 相互独立：
 * {@code snowdrift.xss.escape-parameters=true} 开关参数与 QueryString，
 * {@code snowdrift.xss.escape-headers=true} 开关 Header。
 * 默认关闭是因为转义会改写数据本身 —— 参数（{@code a&b} → {@code a&amp;b}，污染审计/日志里的入参、
 * 破坏签名与令牌解析）与 Header（{@code If-None-Match} 的引号被破坏、令牌里的 {@code &} 被改写）
 * 都属同一类危害，而 XSS 是输出上下文问题。
 * </p>
 * <p>
 * 说明：本过滤器不读取、也不清洗请求体原始输入流；
 * {@code @RequestBody} 的清洗由 {@link com.snowdrift.framework.web.xss.XssJsonBodyAdvice}
 * 在 Spring MVC 层完成，且仅覆盖 String / Map / Collection / 数组等顶层结构，
 * 不递归到深层 POJO / DTO 字段。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class XssFilter extends OncePerRequestFilter {

    private final XssProperties xssProperties;
    private final XssCleaner xssCleaner;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(XssProperties xssProperties, XssCleaner xssCleaner) {
        this.xssProperties = xssProperties;
        this.xssCleaner = xssCleaner;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // shouldNotFilter 已确认启用且未排除，直接包装即可
        request = new XssRequestWrapper(request, xssCleaner,
                Boolean.TRUE.equals(xssProperties.getEscapeParameters()),
                Boolean.TRUE.equals(xssProperties.getEscapeHeaders()));
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (Boolean.FALSE.equals(xssProperties.getEnabled())) {
            return true;
        }
        List<String> excludes = xssProperties.getExcludePathPatterns();
        if (CollectionUtils.isEmpty(excludes)) {
            return false;
        }
        String path = request.getServletPath();
        return excludes.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
