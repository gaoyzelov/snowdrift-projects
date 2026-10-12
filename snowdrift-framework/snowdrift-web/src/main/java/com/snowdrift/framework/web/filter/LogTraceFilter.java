package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * LogTraceFilter
 * @author gaoyzelov
 * @date 2026/5/7-17:49
 * @description 日志链路追踪过滤器，生成 TraceId 并写入响应头 X-Trace-Id
 * @since 1.0.0
 */
@Slf4j
public class LogTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    /**
     * 入站 traceId 的可接受形式：仅字母/数字与 {@code . _ : -}，长度 1~64 位。
     * <p>
     * 该值是不可信输入，且会被写进 MDC（随每一条日志输出）与响应头：
     * 含换行可伪造出完整的假日志行、含控制字符会污染日志结构，超长值则随每条日志与每个响应扩散。
     * 不合法时不报错，改为使用服务端生成的 traceId（并留下 WARN），
     * 既不影响链路可用性，也不让上游的脏值进入本服务的日志。
     * </p>
     */
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 复用入站 X-Trace-Id（网关/上游传递的链路 ID），否则生成新的
        String traceId = request.getHeader(TRACE_HEADER);
        if (StringUtils.isNotBlank(traceId) && SAFE_TRACE_ID.matcher(traceId).matches()) {
            LogTraceUtil.setTraceId(traceId);
        } else {
            if (StringUtils.isNotBlank(traceId)) {
                log.warn("入站 {} 不合法（仅允许字母/数字/._:- 且不超过 64 位），"
                        + "已改用服务端生成的 traceId，不再复用传输值", TRACE_HEADER);
            }
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
