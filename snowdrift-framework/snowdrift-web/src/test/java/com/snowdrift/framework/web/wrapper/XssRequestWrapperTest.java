package com.snowdrift.framework.web.wrapper;

import com.snowdrift.framework.web.xss.SimpleXssCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XssRequestWrapperTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XSS 请求包装器黑盒测试：参数/多值参数/参数 Map/Header/QueryString 的清洗、null 安全与参数 Map 只读
 * @since 1.0.0
 */
class XssRequestWrapperTest {

    private XssRequestWrapper wrapper(MockHttpServletRequest request) {
        return new XssRequestWrapper(request, new SimpleXssCleaner());
    }

    @Test
    @DisplayName("getParameter 清洗特殊字符")
    void cleansParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<script>");

        assertThat(wrapper(request).getParameter("p")).isEqualTo("&lt;script&gt;");
    }

    @Test
    @DisplayName("getParameterValues 逐值清洗；不存在的参数返回 null")
    void cleansParameterValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<a>", "<b>");

        XssRequestWrapper wrapper = wrapper(request);
        assertThat(wrapper.getParameterValues("p")).containsExactly("&lt;a&gt;", "&lt;b&gt;");
        assertThat(wrapper.getParameterValues("missing")).isNull();
        assertThat(wrapper.getParameter("missing")).isNull();
    }

    @Test
    @DisplayName("getParameterMap 清洗 value 且只读")
    void cleansParameterMapAndReadOnly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<x>");

        XssRequestWrapper wrapper = wrapper(request);
        assertThat(wrapper.getParameterMap().get("p")).containsExactly("&lt;x&gt;");
        assertThatThrownBy(() -> wrapper.getParameterMap().put("k", new String[]{"v"}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getHeader 清洗；不存在的 Header 返回 null")
    void cleansHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("h", "<b>bold</b>");

        XssRequestWrapper wrapper = wrapper(request);
        assertThat(wrapper.getHeader("h")).isEqualTo("&lt;b&gt;bold&lt;/b&gt;");
        assertThat(wrapper.getHeader("missing")).isNull();
    }

    @Test
    @DisplayName("getQueryString 清洗；无 QueryString 返回 null")
    void cleansQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("q=<x>");

        assertThat(wrapper(request).getQueryString()).isEqualTo("q=&lt;x&gt;");
        assertThat(wrapper(new MockHttpServletRequest()).getQueryString()).isNull();
    }
}
