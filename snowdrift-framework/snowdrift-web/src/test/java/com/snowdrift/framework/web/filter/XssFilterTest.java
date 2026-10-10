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
    @DisplayName("doFilter：启用时包装为 XssRequestWrapper，参数被清洗")
    void wrapsAndCleansWhenEnabled() throws Exception {
        MockHttpServletRequest request = reqWithServletPath("/api");
        request.setParameter("p", "<x>");
        AtomicReference<ServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(req);

        filter(props(true, List.of())).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isInstanceOf(XssRequestWrapper.class);
        assertThat(seen.get().getParameter("p")).isEqualTo("&lt;x&gt;");
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
