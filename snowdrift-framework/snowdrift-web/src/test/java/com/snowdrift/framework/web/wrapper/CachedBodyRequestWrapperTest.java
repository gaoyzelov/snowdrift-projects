package com.snowdrift.framework.web.wrapper;

import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CachedBodyRequestWrapperTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 请求体缓存包装器黑盒测试：Body 重复读取、getReader 字符集、ServletInputStream 契约
 * （isReady/isFinished/available）、表单参数解析（QueryString+Body 合并/多值/URL 解码/字符集）与非表单透传
 * @since 1.0.0
 */
class CachedBodyRequestWrapperTest {

    private MockHttpServletRequest jsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    @Test
    @DisplayName("缓存 Body 后可多次读取，内容一致")
    void bodyReadableMultipleTimes() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest("{\"a\":1}"));

        assertThat(new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(wrapper.getCachedBody()).isEqualTo("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getContentLength/Long 返回缓存字节长度")
    void contentLengthReflectsCachedBody() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest("hello"));

        assertThat(wrapper.getContentLength()).isEqualTo(5);
        assertThat(wrapper.getContentLengthLong()).isEqualTo(5L);
    }

    @Test
    @DisplayName("ServletInputStream 契约：isReady=true；读尽前 isFinished=false/available=剩余；读尽后 isFinished=true/available=0")
    void servletInputStreamContract() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest("hello"));
        ServletInputStream in = wrapper.getInputStream();

        assertThat(in.isReady()).isTrue();
        assertThat(in.isFinished()).isFalse();
        assertThat(in.available()).isEqualTo(5);

        assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");

        assertThat(in.isFinished()).isTrue();
        assertThat(in.available()).isZero();
        assertThat(in.read()).isEqualTo(-1);
    }

    @Test
    @DisplayName("部分读取后 available 返回剩余字节、isFinished 仍为 false")
    void availableDecrementsOnPartialRead() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest("hello"));
        ServletInputStream in = wrapper.getInputStream();

        in.read();
        in.read();

        assertThat(in.available()).isEqualTo(3);
        assertThat(in.isFinished()).isFalse();
    }

    @Test
    @DisplayName("空 Body：getContentLength=0，流立即 finished")
    void emptyBody() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest(""));

        assertThat(wrapper.getContentLengthLong()).isZero();
        ServletInputStream in = wrapper.getInputStream();
        assertThat(in.isFinished()).isTrue();
        assertThat(in.read()).isEqualTo(-1);
    }

    @Test
    @DisplayName("getReader 按请求字符集解码（UTF-8 多字节不被平台默认编码破坏）")
    void readerUsesRequestCharset() throws IOException {
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(jsonRequest("你好，世界"));

        String line = wrapper.getReader().readLine();

        assertThat(line).isEqualTo("你好，世界");
    }

    @Test
    @DisplayName("表单请求：合并 QueryString 与 Body 参数，URL 解码，支持多值")
    void formParametersMergedAndDecoded() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        request.setCharacterEncoding("UTF-8");
        request.setContent("name=%E5%BC%A0%E4%B8%89&age=20&tag=a&tag=b".getBytes(StandardCharsets.UTF_8));
        request.addParameter("q", "1"); // 模拟 QueryString 参数（super.getParameterMap）

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getParameter("name")).isEqualTo("张三");
        assertThat(wrapper.getParameter("age")).isEqualTo("20");
        assertThat(wrapper.getParameter("q")).isEqualTo("1");
        assertThat(wrapper.getParameterValues("tag")).containsExactly("a", "b");
        assertThat(wrapper.getParameterMap()).containsKeys("name", "age", "q", "tag");
        assertThat(Collections.list(wrapper.getParameterNames())).contains("name", "age", "q", "tag");
    }

    @Test
    @DisplayName("表单参数 Map 只读，外部修改抛 UnsupportedOperationException")
    void formParameterMapIsReadOnly() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/x-www-form-urlencoded");
        request.setCharacterEncoding("UTF-8");
        request.setContent("a=b".getBytes(StandardCharsets.UTF_8));

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> wrapper.getParameterMap().put("x", new String[]{"y"}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("表单请求 Body 为空时仅返回 QueryString 参数")
    void emptyFormBodyKeepsQueryParams() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/x-www-form-urlencoded");
        request.setCharacterEncoding("UTF-8");
        request.setContent(new byte[0]);
        request.addParameter("q", "1");

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getParameterMap()).containsOnlyKeys("q");
    }

    @Test
    @DisplayName("非表单请求：getParameter/getParameterMap 透传 super，不解析 Body")
    void nonFormDelegatesToSuper() throws IOException {
        MockHttpServletRequest request = jsonRequest("{\"x\":1}");
        request.addParameter("q", "1");

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getParameter("q")).isEqualTo("1");
        assertThat(wrapper.getParameterMap()).containsOnlyKeys("q");
        // Body 未被当作表单解析
        assertThat(wrapper.getParameter("x")).isNull();
    }

    @Test
    @DisplayName("非法字符编码名回退 UTF-8 解码表单 Body")
    void invalidCharsetFallsBackToUtf8() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/x-www-form-urlencoded");
        request.setCharacterEncoding("NOT-A-CHARSET");
        request.setContent("name=%E5%BC%A0%E4%B8%89".getBytes(StandardCharsets.UTF_8));

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getParameter("name")).isEqualTo("张三");
    }
}
