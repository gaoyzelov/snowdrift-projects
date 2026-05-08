package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.IpUtil;
import com.snowdrift.framework.common.util.ServletUtil;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * HttpContextFilter
 * @author 83674
 * @date 2026/5/7-17:47
 * @description Http请求上下文过滤器
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@WebFilter(urlPatterns = "/*")
public class HttpContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String ip = IpUtil.getIp(httpRequest);
                HttpContext context = HttpContext.builder()
                        .ip(ip)
                        .ipAddr(IpUtil.getIpAddr(ip, StrConst.SPACE))
                        .userAgent(ServletUtil.getUserAgent(httpRequest))
                        .uri(httpRequest.getRequestURI())
                        .method(httpRequest.getMethod())
                        .paramMap(ServletUtil.getParamMap(httpRequest))
                        .build();
                HttpContextHolder.setContext(context);
            }
            chain.doFilter(request, response);
        }finally {
            HttpContextHolder.clear();
        }
    }
}
