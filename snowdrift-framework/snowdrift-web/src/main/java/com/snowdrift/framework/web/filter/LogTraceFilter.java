package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * LogTraceFilter
 * @author gaoyzelov
 * @date 2026/5/7-17:49
 * @description 日志链路追踪过滤器，生成 TraceId 并写入响应头 X-Trace-Id
 * @since 1.0.0
 */
public class LogTraceFilter implements Filter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 生成 TraceId
        LogTraceUtil.setTraceId();
        // 写入响应头，方便客户端关联报错
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(TRACE_HEADER, LogTraceUtil.getTraceId());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 请求结束清理 TraceId
            LogTraceUtil.clearTraceId();
        }
    }

}
