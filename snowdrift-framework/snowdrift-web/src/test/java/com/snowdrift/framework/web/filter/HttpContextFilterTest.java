package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
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
 * HttpContextFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description HTTP 上下文过滤器黑盒测试：链内填充 HttpContext（IP/URI/method/UA/参数），请求结束（含异常）在 finally 清理
 * @since 1.0.0
 */
class HttpContextFilterTest {

    private final HttpContextFilter filter = new HttpContextFilter();

    @AfterEach
    void tearDown() {
        HttpContextHolder.clear();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/order");
        request.setMethod("GET");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "junit-agent");
        request.addParameter("id", "10");
        return request;
    }

    @Test
    @DisplayName("链内填充 HttpContext：uri/method/ip/userAgent/paramMap 正确")
    void populatesContextDuringChain() throws Exception {
        AtomicReference<HttpContext> captured = new AtomicReference<>();
        FilterChain chain = (req, resp) -> captured.set(HttpContextHolder.getContext());

        filter.doFilter(request(), new MockHttpServletResponse(), chain);

        HttpContext context = captured.get();
        assertThat(context).isNotNull();
        assertThat(context.getUri()).isEqualTo("/api/order");
        assertThat(context.getMethod()).isEqualTo("GET");
        assertThat(context.getIp()).isEqualTo("127.0.0.1");
        assertThat(context.getUserAgent()).isEqualTo("junit-agent");
        assertThat(context.getParamMap()).containsEntry("id", "10");
    }

    @Test
    @DisplayName("请求正常结束后清理上下文（防泄漏/串号）")
    void clearsContextAfterCompletion() throws Exception {
        filter.doFilter(request(), new MockHttpServletResponse(), (req, resp) -> {
        });

        assertThat(HttpContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("下游抛异常时异常上抛，且 finally 仍清理上下文")
    void clearsContextOnException() {
        FilterChain chain = (req, resp) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request(), new MockHttpServletResponse(), chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(HttpContextHolder.getContext()).isNull();
    }
}
