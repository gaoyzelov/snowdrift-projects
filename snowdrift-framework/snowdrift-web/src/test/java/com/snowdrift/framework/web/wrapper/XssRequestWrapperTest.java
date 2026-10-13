package com.snowdrift.framework.web.wrapper;

import com.snowdrift.framework.web.xss.SimpleXssCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XssRequestWrapperTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XSS 请求包装器黑盒测试：默认不改写任何输入侧数据（参数/Header/QueryString），
 *              两个开关分别显式开启参数与 Header 清洗；Header 的 getHeader/getHeaders 口径一致、
 *              Header 名不转义；参数 Map 只读
 * @since 1.0.0
 */
class XssRequestWrapperTest {

    /** 参数清洗开启（旧行为回退用） */
    private XssRequestWrapper paramsEscaped(MockHttpServletRequest request) {
        return new XssRequestWrapper(request, new SimpleXssCleaner(), true, false);
    }

    /** Header 清洗开启 */
    private XssRequestWrapper headersEscaped(MockHttpServletRequest request) {
        return new XssRequestWrapper(request, new SimpleXssCleaner(), false, true);
    }

    /** 两个开关都开启（完整旧行为） */
    private XssRequestWrapper allEscaped(MockHttpServletRequest request) {
        return new XssRequestWrapper(request, new SimpleXssCleaner(), true, true);
    }

    // ========== 默认：不改写任何输入侧数据 ==========

    @Test
    @DisplayName("默认不清洗请求参数：合法含 & < 的业务参数不得被改写（XSS 属输出侧问题）")
    void parametersAreNotEscapedByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("q", "a&b<c>");

        XssRequestWrapper wrapper = new XssRequestWrapper(request, new SimpleXssCleaner());

        assertThat(wrapper.getParameter("q")).isEqualTo("a&b<c>");
        assertThat(wrapper.getParameterValues("q")).containsExactly("a&b<c>");
        assertThat(wrapper.getParameterMap().get("q")).containsExactly("a&b<c>");
    }

    @Test
    @DisplayName("默认不清洗 Header：带引号/& 的协议值（If-None-Match、Authorization）不得被改写")
    void headersAreNotEscapedByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", "\"v1\"");
        request.addHeader("Authorization", "Bearer a.b&c");
        request.addHeader("X-Multi", "a&b");
        request.addHeader("X-Multi", "c<d");

        XssRequestWrapper wrapper = new XssRequestWrapper(request, new SimpleXssCleaner());

        assertThat(wrapper.getHeader("If-None-Match")).isEqualTo("\"v1\"");
        assertThat(wrapper.getHeader("Authorization")).isEqualTo("Bearer a.b&c");
        assertThat(Collections.list(wrapper.getHeaders("X-Multi"))).containsExactly("a&b", "c<d");
    }

    @Test
    @DisplayName("默认不清洗 QueryString：下游对原始查询串做签名校验时不得被改写")
    void queryStringIsNotEscapedByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("sign=a&b");

        assertThat(new XssRequestWrapper(request, new SimpleXssCleaner()).getQueryString())
                .isEqualTo("sign=a&b");
    }

    // ========== 显式开启 ==========

    @Test
    @DisplayName("显式开启 escapeParameters 时恢复旧行为（供依赖它的下游平滑迁移）")
    void parametersEscapingRestoredWhenExplicitlyEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("q", "a&b");
        request.setQueryString("q=a&b");

        XssRequestWrapper wrapper = paramsEscaped(request);

        assertThat(wrapper.getParameter("q")).isEqualTo("a&amp;b");
        assertThat(wrapper.getQueryString()).isEqualTo("q=a&amp;b");
    }

    @Test
    @DisplayName("显式开启 escapeHeaders 时清洗 Header，且 getHeader/getHeaders 口径一致")
    void headerEscapingAppliesToBothAccessors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Note", "<b>bold</b>");
        request.addHeader("X-Multi", "<a>");
        request.addHeader("X-Multi", "<c>");

        XssRequestWrapper wrapper = headersEscaped(request);

        assertThat(wrapper.getHeader("X-Note")).isEqualTo("&lt;b&gt;bold&lt;/b&gt;");
        assertThat(wrapper.getHeader("missing")).isNull();
        // 同一个头经两个访问器必须得到同一个值
        assertThat(Collections.list(wrapper.getHeaders("X-Note")))
                .containsExactly(wrapper.getHeader("X-Note"));
        assertThat(Collections.list(wrapper.getHeaders("X-Multi")))
                .containsExactly("&lt;a&gt;", "&lt;c&gt;");
    }

    @Test
    @DisplayName("Header 名始终不转义：转义后无法再用作查值的键")
    void headerNamesAreNeverEscaped() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Quote", "\"v\"");

        XssRequestWrapper wrapper = allEscaped(request);

        assertThat(Collections.list(wrapper.getHeaderNames())).contains("X-Quote");
        assertThat(wrapper.getHeader("X-Quote")).isEqualTo("&quot;v&quot;");
    }

    // ========== 清洗本身的行为（开关开启时） ==========

    @Test
    @DisplayName("getParameter 清洗特殊字符")
    void cleansParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<script>");

        assertThat(paramsEscaped(request).getParameter("p")).isEqualTo("&lt;script&gt;");
    }

    @Test
    @DisplayName("getParameterValues 逐值清洗；不存在的参数返回 null")
    void cleansParameterValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<a>", "<b>");

        XssRequestWrapper wrapper = paramsEscaped(request);
        assertThat(wrapper.getParameterValues("p")).containsExactly("&lt;a&gt;", "&lt;b&gt;");
        assertThat(wrapper.getParameterValues("missing")).isNull();
        assertThat(wrapper.getParameter("missing")).isNull();
    }

    @Test
    @DisplayName("getParameterMap 清洗 value 且只读")
    void cleansParameterMapAndReadOnly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<x>");

        XssRequestWrapper wrapper = paramsEscaped(request);
        assertThat(wrapper.getParameterMap().get("p")).containsExactly("&lt;x&gt;");
        assertThatThrownBy(() -> wrapper.getParameterMap().put("k", new String[]{"v"}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("未开启参数清洗时参数 Map 仍为只读视图")
    void parameterMapStaysReadOnlyWithoutEscaping() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("p", "<x>");

        XssRequestWrapper wrapper = new XssRequestWrapper(request, new SimpleXssCleaner());

        assertThat(wrapper.getParameterMap().get("p")).containsExactly("<x>");
        assertThatThrownBy(() -> wrapper.getParameterMap().put("k", new String[]{"v"}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
