package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.IpUtil;
import com.snowdrift.framework.common.util.ServletUtil;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HttpContextFilter
 * @author gaoyzelov
 * @date 2026/5/7-17:47
 * @description Http请求上下文过滤器
 * @since 1.0.0
 */
public class HttpContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String ip = IpUtil.getIp(request);
            HttpContext context = HttpContext.builder()
                    .ip(ip)
                    .ipLocation(IpUtil.getIpLocation(ip, StrConst.SPACE))
                    .userAgent(ServletUtil.getUserAgent(request))
                    .uri(request.getRequestURI())
                    .method(request.getMethod())
                    .paramMap(ServletUtil.getParamMap(request))
                    .build();
            HttpContextHolder.setContext(context);
            chain.doFilter(request, response);
        } finally {
            HttpContextHolder.clear();
        }
    }
}
