package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * LogTraceFilter
 * @author 83674
 * @date 2026/5/7-17:49
 * @description 日志链路追踪过滤器
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter(urlPatterns = "/*")
public class LogTraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            // 生成 TraceId
            LogTraceUtil.setTraceId();
            chain.doFilter(request, response);
        } finally {
            // 请求结束清理 TraceId
            LogTraceUtil.clearTraceId();
        }
    }

}
