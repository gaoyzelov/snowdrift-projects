package com.snowdrift.framework.web.filter;

import com.snowdrift.framework.web.properties.XssProperties;
import com.snowdrift.framework.web.wrapper.XssRequestWrapper;
import com.snowdrift.framework.web.xss.SimpleXssCleaner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XssFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XSS 过滤器黑盒测试：shouldNotFilter 的开关/排除路径（Ant 风格）判定，doFilter 启用时包装、关闭时透传
 * @since 1.0.0
 */
class XssFilterTest {

    private XssProperties props(boolean enabled, List<String> excludes) {
        XssProperties properties = new XssProperties();
        properties.setEnabled(enabled);
        properties.setExcludePathPatterns(excludes);
        return properties;
    }

    private XssFilter filter(XssProperties properties) {
        return new XssFilter(properties, new SimpleXssCleaner());
    }

    private MockHttpServletRequest reqWithServletPath(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return request;
    }

    @Test
    @DisplayName("关闭时 shouldNotFilter=true（跳过）")
    void disabledSkips() {
        assertThat(filter(props(false, List.of())).shouldNotFilter(reqWithServletPath("/api")))
                .isTrue();
    }

    @Test
    @DisplayName("启用且无排除路径时 shouldNotFilter=false")
    void enabledNoExcludesFilters() {
        assertThat(filter(props(true, List.of())).shouldNotFilter(reqWithServletPath("/api")))
                .isFalse();
    }

    @Test
    @DisplayName("启用且命中排除路径（Ant /**）时 shouldNotFilter=true")
    void enabledMatchingExcludeSkips() {
        XssFilter filter = filter(props(true, List.of("/admin/richtext/**")));

        assertThat(filter.shouldNotFilter(reqWithServletPath("/admin/richtext/edit"))).isTrue();
        assertThat(filter.shouldNotFilter(reqWithServletPath("/api/order"))).isFalse();
    }

    @Test
    @DisplayName("doFilter：启用时包装为 XssRequestWrapper，但参数与 Header 默认都不被清洗")
    void wrapsWithoutCleaningInputByDefault() throws Exception {
        MockHttpServletRequest request = reqWithServletPath("/api");
        request.setParameter("p", "<x>");
        request.addHeader("X-Note", "<x>");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        filter(props(true, List.of())).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isInstanceOf(XssRequestWrapper.class);
        // 输入侧转义会改写业务数据、破坏协议语义值，默认都不做
        assertThat(seen.get().getParameter("p")).isEqualTo("<x>");
        assertThat(((jakarta.servlet.http.HttpServletRequest) seen.get()).getHeader("X-Note"))
                .isEqualTo("<x>");
    }

    @Test
    @DisplayName("doFilter：显式开启 escape-parameters 时参数与 QueryString 恢复清洗（下游平滑迁移用）")
    void cleansParametersWhenExplicitlyEnabled() throws Exception {
        MockHttpServletRequest request = reqWithServletPath("/api");
        request.setParameter("p", "<x>");
        request.setQueryString("q=<x>");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        XssProperties withEscape = props(true, List.of());
        withEscape.setEscapeParameters(true);
        filter(withEscape).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get().getParameter("p")).isEqualTo("&lt;x&gt;");
        assertThat(((jakarta.servlet.http.HttpServletRequest) seen.get()).getQueryString())
                .isEqualTo("q=&lt;x&gt;");
    }

    @Test
    @DisplayName("doFilter：显式开启 escape-headers 时 Header 恢复清洗")
    void cleansHeadersWhenExplicitlyEnabled() throws Exception {
        MockHttpServletRequest request = reqWithServletPath("/api");
        request.addHeader("X-Note", "<x>");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        XssProperties withEscape = props(true, List.of());
        withEscape.setEscapeHeaders(true);
        filter(withEscape).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(((jakarta.servlet.http.HttpServletRequest) seen.get()).getHeader("X-Note"))
                .isEqualTo("&lt;x&gt;");
    }

    @Test
    @DisplayName("doFilter：关闭时原样透传，不包装")
    void passesThroughWhenDisabled() throws Exception {
        MockHttpServletRequest request = reqWithServletPath("/api");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        filter(props(false, List.of())).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isSameAs(request);
    }
}
