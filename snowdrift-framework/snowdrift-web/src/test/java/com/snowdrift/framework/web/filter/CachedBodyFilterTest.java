package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.web.wrapper.CachedBodyRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CachedBodyFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 请求体缓存过滤器黑盒测试：shouldNotFilter 的 multipart/octet-stream/未知长度/超大包/正常包判定，
 * 以及 doFilter 对可缓存请求包装为 CachedBodyRequestWrapper、对不缓存请求原样透传
 * @since 1.0.0
 */
class CachedBodyFilterTest {

    private final CachedBodyFilter filter = new CachedBodyFilter();

    private HttpServletRequest reqWith(String contentType, long contentLength) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContentType()).thenReturn(contentType);
        when(request.getContentLengthLong()).thenReturn(contentLength);
        return request;
    }

    @Test
    @DisplayName("multipart 请求不缓存")
    void skipsMultipart() {
        assertThat(filter.shouldNotFilter(reqWith("multipart/form-data; boundary=x", 100L))).isTrue();
    }

    @Test
    @DisplayName("application/octet-stream 不缓存")
    void skipsOctetStream() {
        assertThat(filter.shouldNotFilter(reqWith("application/octet-stream", 100L))).isTrue();
    }

    @Test
    @DisplayName("未知长度（chunked，contentLength<0）不缓存")
    void skipsUnknownLength() {
        assertThat(filter.shouldNotFilter(reqWith("application/json", -1L))).isTrue();
    }

    @Test
    @DisplayName("超过 5MB 的大包不缓存")
    void skipsOversize() {
        long over = 5 * 1024 * 1024L + 1;
        assertThat(filter.shouldNotFilter(reqWith("application/json", over))).isTrue();
    }

    @Test
    @DisplayName("正常有界 JSON 包需要缓存（shouldNotFilter=false），边界 5MB 恰好缓存")
    void cachesNormalAndBoundary() {
        assertThat(filter.shouldNotFilter(reqWith("application/json", 1024L))).isFalse();
        assertThat(filter.shouldNotFilter(reqWith("application/json", 5 * 1024 * 1024L))).isFalse();
        assertThat(filter.shouldNotFilter(reqWith(null, 0L))).isFalse();
    }

    @Test
    @DisplayName("doFilter：可缓存请求被包装为 CachedBodyRequestWrapper 传给下游")
    void wrapsCacheableRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{}".getBytes());
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isInstanceOf(CachedBodyRequestWrapper.class);
    }

    @Test
    @DisplayName("doFilter：不缓存的请求（multipart）原样透传，不包装")
    void passesThroughNonCacheableRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("multipart/form-data");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isSameAs(request);
    }

    @Test
    @DisplayName("包装后下游可读取到缓存的 Body")
    void downstreamCanReadCachedBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"k\":\"v\"}".getBytes());
        AtomicReference<String> body = new AtomicReference<>();
        FilterChain chain = (req, resp) ->
                body.set(new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(body.get()).isEqualTo("{\"k\":\"v\"}");
    }

    @Test
    @DisplayName("ServletResponse 参数被透传给下游（不被过滤器改写）")
    void passesResponseThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletResponse> seenResp = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seenResp.set(resp);

        filter.doFilter(request, response, chain);

        assertThat(seenResp.get()).isSameAs(response);
    }
}
