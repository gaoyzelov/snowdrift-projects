package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LogTraceFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 链路追踪过滤器黑盒测试：复用入站 X-Trace-Id、无入站时生成、写入响应头、请求结束（含异常）在 finally 清理 MDC
 * @since 1.0.0
 */
class LogTraceFilterTest {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final LogTraceFilter filter = new LogTraceFilter();

    @AfterEach
    void tearDown() {
        LogTraceUtil.clearTraceId();
    }

    @Test
    @DisplayName("复用入站 X-Trace-Id：MDC 与响应头均为入站值")
    void reusesInboundTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TRACE_HEADER, "inbound-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringChain = new AtomicReference<>();
        FilterChain chain = (req, resp) -> duringChain.set(LogTraceUtil.getTraceId());

        filter.doFilter(request, response, chain);

        assertThat(duringChain.get()).isEqualTo("inbound-123");
        assertThat(response.getHeader(TRACE_HEADER)).isEqualTo("inbound-123");
    }

    @Test
    @DisplayName("无入站头时生成非空 traceId，写入 MDC 与响应头")
    void generatesTraceIdWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringChain = new AtomicReference<>();
        FilterChain chain = (req, resp) -> duringChain.set(LogTraceUtil.getTraceId());

        filter.doFilter(request, response, chain);

        assertThat(duringChain.get()).isNotBlank();
        assertThat(response.getHeader(TRACE_HEADER)).isEqualTo(duringChain.get());
    }

    @Test
    @DisplayName("入站头为空白时视为无，生成新 traceId")
    void blankInboundHeaderRegenerates() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TRACE_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> {
        });

        assertThat(response.getHeader(TRACE_HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("请求正常结束后清理 MDC（防泄漏）")
    void clearsTraceIdAfterCompletion() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, resp) -> {
        });

        assertThat(LogTraceUtil.getTraceId()).isNull();
    }

    @Test
    @DisplayName("下游抛异常时异常上抛，且 finally 仍清理 MDC")
    void clearsTraceIdOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        FilterChain chain = (req, resp) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(LogTraceUtil.getTraceId()).isNull();
    }
}
