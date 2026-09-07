package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * LogTraceFilter
 * @author gaoyzelov
 * @date 2026/5/7-17:49
 * @description 日志链路追踪过滤器，生成 TraceId 并写入响应头 X-Trace-Id
 * @since 1.0.0
 */
public class LogTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 复用入站 X-Trace-Id（网关/上游传递的链路 ID），否则生成新的
        String inbound = request.getHeader(TRACE_HEADER);
        if (StringUtils.isNotBlank(inbound)) {
            LogTraceUtil.setTraceId(inbound);
        } else {
            LogTraceUtil.setTraceId();
        }
        // 写入响应头，方便客户端关联报错
        response.setHeader(TRACE_HEADER, LogTraceUtil.getTraceId());
        try {
            chain.doFilter(request, response);
        } finally {
            // 请求结束清理 TraceId
            LogTraceUtil.clearTraceId();
        }
    }
}
