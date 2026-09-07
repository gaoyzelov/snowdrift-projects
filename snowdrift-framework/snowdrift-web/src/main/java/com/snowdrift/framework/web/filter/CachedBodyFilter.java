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

    /**
     * 超过该体积的请求体不缓存（防止整包读入内存造成 DoS），交由下游按原始流消费一次
     */
    private static final long MAX_CACHE_BODY_BYTES = 5 * 1024 * 1024L;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 文件上传/二进制流不缓存，避免大文件整包入内存
        String contentType = request.getContentType();
        if (contentType != null
                && (contentType.startsWith("multipart/") || contentType.startsWith("application/octet-stream"))) {
            return true;
        }
        return request.getContentLengthLong() > MAX_CACHE_BODY_BYTES;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request = new CachedBodyRequestWrapper(request);
        chain.doFilter(request, response);
    }
}
