package com.snowdrift.framework.web.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogTraceFilterInboundTraceIdTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 入站 X-Trace-Id 是不可信输入：它会被写进 MDC（每一条日志）与响应头，
 *              必须校验字符集与长度，否则可注入伪造日志行、或把超长值带进所有日志与响应
 * @since 1.0.0
 */
class LogTraceFilterInboundTraceIdTest {

    private static final String HEADER = "X-Trace-Id";

    private final LogTraceFilter filter = new LogTraceFilter();

    /** 跑一遍过滤器，返回写进响应头的 traceId */
    private String traceIdAfter(String inboundHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        if (inboundHeader != null) {
            request.addHeader(HEADER, inboundHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response.getHeader(HEADER);
    }

    @Test
    @DisplayName("合法入站 traceId 被复用（保持网关→服务的链路串联）")
    void validInboundTraceIdIsReused() throws Exception {
        assertThat(traceIdAfter("abc-123_XY.Z:9")).isEqualTo("abc-123_XY.Z:9");
    }

    @Test
    @DisplayName("含换行的入站 traceId 被拒：否则可在日志里伪造出完整的假日志行")
    void newlineInjectedTraceIdRejected() throws Exception {
        String forged = "ok\n2026-01-01 00:00:00.000 ERROR 伪造日志行";

        String traceId = traceIdAfter(forged);

        assertThat(traceId).doesNotContain("伪造日志行").doesNotContain("\n").isNotEqualTo(forged);
    }

    @Test
    @DisplayName("超长入站 traceId 被拒：无长度上限会随每一条日志与每个响应扩散")
    void oversizedTraceIdRejected() throws Exception {
        String huge = "a".repeat(4096);

        String traceId = traceIdAfter(huge);

        assertThat(traceId).hasSizeLessThan(128).isNotEqualTo(huge);
    }

    @Test
    @DisplayName("含空格/引号等非白名单字符的入站 traceId 被拒")
    void charsetViolationRejected() throws Exception {
        String unsafe = "a b\"c<d>";

        assertThat(traceIdAfter(unsafe)).isNotEqualTo(unsafe);
    }

    @Test
    @DisplayName("未携带入站 traceId 时由服务端生成非空值")
    void generatesWhenHeaderAbsent() throws Exception {
        assertThat(traceIdAfter(null)).isNotBlank();
    }
}
