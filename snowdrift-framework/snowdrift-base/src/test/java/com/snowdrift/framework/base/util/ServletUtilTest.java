package com.snowdrift.framework.base.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ServletUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Servlet 工具类单元测试
 * @since 1.0.0
 */
class ServletUtilTest {

    @Test
    @DisplayName("getUserAgent：缺失时返回空串而不是 null")
    void userAgent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        assertThat(ServletUtil.getUserAgent(request)).isEqualTo("Mozilla/5.0");

        when(request.getHeader("User-Agent")).thenReturn(null);
        assertThat(ServletUtil.getUserAgent(request)).isEmpty();
    }

    @Test
    @DisplayName("getHeaderMap：保留同名头的第一个值，无头时返回空 Map")
    void headerMap() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("A", "B")));
        when(request.getHeader("A")).thenReturn("1");
        when(request.getHeader("B")).thenReturn("2");

        assertThat(ServletUtil.getHeaderMap(request)).containsExactlyInAnyOrderEntriesOf(Map.of("A", "1", "B", "2"));

        HttpServletRequest withoutHeaders = mock(HttpServletRequest.class);
        when(withoutHeaders.getHeaderNames()).thenReturn(null);
        assertThat(ServletUtil.getHeaderMap(withoutHeaders)).isEmpty();
    }

    @Test
    @DisplayName("getParamMap：多值参数以逗号拼接（有损视图）")
    void paramMap() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String[]> parameterMap = new LinkedHashMap<>();
        parameterMap.put("ids", new String[]{"1", "2", "3"});
        parameterMap.put("name", new String[]{"tom"});
        when(request.getParameterMap()).thenReturn(parameterMap);

        Map<String, String> params = ServletUtil.getParamMap(request);
        assertThat(params).containsEntry("ids", "1,2,3").containsEntry("name", "tom");
    }

    @Test
    @DisplayName("writeJsonResponse：设置状态码、JSON 内容类型与 UTF-8，并 flush 但不关闭 writer")
    void writeJsonResponse() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);
        when(response.getWriter()).thenReturn(writer);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", 1001);
        payload.put("msg", "未授权");

        ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, payload);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        // 写出与 flush 各取一次 writer
        verify(response, org.mockito.Mockito.atLeastOnce()).getWriter();

        assertThat(body.toString()).isEqualTo("{\"code\":1001,\"msg\":\"未授权\"}");

        // flush 后 writer 仍可继续写入，说明未调用 close
        writer.write("tail");
        assertThat(body.toString()).endsWith("tail");
    }

    @Test
    @DisplayName("writeJsonResponse：非 Map 对象按 Fastjson2 默认规则序列化")
    void writeJsonResponseWithPojo() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_OK, List.of(1, 2));

        assertThat(body.toString()).isEqualTo("[1,2]");
    }

    @Test
    @DisplayName("writeJsonResponse：获取 writer 失败时向外传播 IOException，且已先设置状态与内容类型")
    void propagatesIoException() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("broken pipe"));

        assertThatThrownBy(() -> ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_OK, "x"))
                .isInstanceOf(IOException.class)
                .hasMessage("broken pipe");

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
    }
}
