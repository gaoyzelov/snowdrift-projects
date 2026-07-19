package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.web.config.XssProperties;
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
 * XSS 防护过滤器 — 对请求参数和请求头等做 XSS 清洗。
 * <p>
 * 依赖 {@link CachedBodyFilter} 先缓存 Body，本过滤器在缓存基础上做 XSS 清洗。
 * 通过 {@code snowdrift.web.xss.enabled=true} 启用，{@code exclude-path-patterns} 排除指定路径。
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
