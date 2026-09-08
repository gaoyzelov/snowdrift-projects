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
 * XSS 防护过滤器 — 对请求参数、请求头和查询字符串做 XSS 清洗。
 * <p>
 * 通过 {@code snowdrift.xss.enabled=true} 启用（默认关闭），
 * {@code exclude-path-patterns} 排除指定路径。
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
        request = new XssRequestWrapper(request, xssCleaner);
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
