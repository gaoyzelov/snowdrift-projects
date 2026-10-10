package com.snowdrift.framework.web.xss;

import com.snowdrift.framework.web.properties.XssProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * XssJsonBodyAdviceTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XSS JSON Body 清洗 Advice 黑盒测试：supports 的开关/排除路径判定，afterBodyRead 对
 * String/Map(仅 value)/List/数组/嵌套结构的清洗，Set 与 POJO 的既定“不清洗”边界，以及不可变集合静默跳过
 * @since 1.0.0
 */
class XssJsonBodyAdviceTest {

    private XssJsonBodyAdvice advice(boolean enabled, List<String> excludes) {
        XssProperties properties = new XssProperties();
        properties.setEnabled(enabled);
        properties.setExcludePathPatterns(excludes);
        return new XssJsonBodyAdvice(properties, new SimpleXssCleaner());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ========== supports ==========

    @Test
    @DisplayName("supports：关闭时 false")
    void supportsDisabled() {
        assertThat(advice(false, List.of()).supports(null, null, null)).isFalse();
    }

    @Test
    @DisplayName("supports：启用且无排除路径时 true")
    void supportsEnabledNoExcludes() {
        assertThat(advice(true, List.of()).supports(null, null, null)).isTrue();
    }

    @Test
    @DisplayName("supports：命中排除路径时 false，未命中时 true")
    void supportsExcludePath() {
        XssJsonBodyAdvice advice = advice(true, List.of("/admin/richtext/**"));

        MockHttpServletRequest excluded = new MockHttpServletRequest();
        excluded.setServletPath("/admin/richtext/edit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(excluded));
        assertThat(advice.supports(null, null, null)).isFalse();

        MockHttpServletRequest normal = new MockHttpServletRequest();
        normal.setServletPath("/api/order");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(normal));
        assertThat(advice.supports(null, null, null)).isTrue();
    }

    @Test
    @DisplayName("supports：无请求上下文（非 HTTP 线程）时不因排除路径报错，返回 true")
    void supportsWithoutRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        assertThat(advice(true, List.of("/admin/**")).supports(null, null, null)).isTrue();
    }

    // ========== afterBodyRead ==========

    @Test
    @DisplayName("null body 返回 null")
    void nullBody() {
        assertThat(advice(true, List.of()).afterBodyRead(null, null, null, null, null)).isNull();
    }

    @Test
    @DisplayName("String 直接清洗")
    void cleansString() {
        Object result = advice(true, List.of()).afterBodyRead("<x>", null, null, null, null);

        assertThat(result).isEqualTo("&lt;x&gt;");
    }

    @Test
    @DisplayName("Map：清洗 value，不清洗 key")
    void cleansMapValuesNotKeys() {
        Map<String, Object> body = new HashMap<>();
        body.put("<k>", "<v>");

        advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(body).containsEntry("<k>", "&lt;v&gt;");
    }

    @Test
    @DisplayName("List：原地逐元素清洗")
    void cleansListInPlace() {
        List<Object> body = new ArrayList<>(List.of("<a>", "<b>"));

        advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(body).containsExactly("&lt;a&gt;", "&lt;b&gt;");
    }

    @Test
    @DisplayName("数组：原地逐元素清洗")
    void cleansArrayInPlace() {
        String[] body = {"<a>", "<b>"};

        advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(body).containsExactly("&lt;a&gt;", "&lt;b&gt;");
    }

    @Test
    @DisplayName("嵌套结构：Map 内 List、List 内 Map 递归清洗")
    void cleansNestedStructures() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("k", "<v>");
        List<Object> list = new ArrayList<>(List.of("<a>"));
        Map<String, Object> body = new HashMap<>();
        body.put("list", list);
        List<Object> mapList = new ArrayList<>();
        mapList.add(inner);
        body.put("mapList", mapList);

        advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(list).containsExactly("&lt;a&gt;");
        assertThat(inner).containsEntry("k", "&lt;v&gt;");
    }

    @Test
    @DisplayName("既定边界：Set 中的 String 元素不被清洗（无法安全原地替换）")
    void setElementsNotCleaned() {
        Set<String> body = new HashSet<>(List.of("<a>"));

        advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(body).containsExactly("<a>");
    }

    @Test
    @DisplayName("既定边界：复杂 POJO 整体跳过（不做反射），字段不变")
    void pojoSkipped() {
        Dto body = new Dto();

        Object result = advice(true, List.of()).afterBodyRead(body, null, null, null, null);

        assertThat(result).isSameAs(body);
        assertThat(body.name).isEqualTo("<x>");
    }

    @Test
    @DisplayName("非 String 标量（数字/布尔）原样返回")
    void scalarsUnchanged() {
        assertThat(advice(true, List.of()).afterBodyRead(123, null, null, null, null)).isEqualTo(123);
        assertThat(advice(true, List.of()).afterBodyRead(Boolean.TRUE, null, null, null, null)).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("不可变 Map / 不可变 List 静默跳过，不抛异常")
    void immutableCollectionsSkipped() {
        Map<String, Object> immutableMap = Map.of("k", "<v>");
        List<Object> immutableList = List.of("<a>");

        XssJsonBodyAdvice advice = advice(true, List.of());
        assertThatCode(() -> advice.afterBodyRead(immutableMap, null, null, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> advice.afterBodyRead(immutableList, null, null, null, null)).doesNotThrowAnyException();
        assertThat(immutableMap).containsEntry("k", "<v>");
        assertThat(immutableList).containsExactly("<a>");
    }

    static class Dto {
        String name = "<x>";
    }
}
