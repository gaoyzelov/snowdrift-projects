package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.web.wrapper.CachedBodyRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求体缓存过滤器 — 一次性读取 Body 并缓存，支持下游多次消费。
 * <p>
 * 在 Filter 链最前端执行，确保后续任何 Filter、拦截器、Controller
 * 都可以通过 {@code request.getInputStream()} / {@code getReader()} 重复读取请求体。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class CachedBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request = new CachedBodyRequestWrapper(request);
        chain.doFilter(request, response);
    }
}
