package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.security.annotation.Anonymous;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AnonymousScannerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description @Anonymous 路径扫描黑盒测试：仅收集标注方法的原始模板、去重、忽略未标注方法
 * @since 1.0.0
 */
class AnonymousScannerTest {

    static class TestController {
        @Anonymous
        public void anon() {
        }

        @Anonymous
        public void multiPath() {
        }

        public void secured() {
        }
    }

    private static RequestMappingInfo info(String... patterns) {
        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
        return RequestMappingInfo.paths(patterns).options(options).build();
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    @Test
    @DisplayName("仅收集标注 @Anonymous 方法的 URL 模板，未标注方法被忽略")
    void scansOnlyAnonymousMethods() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/login"), handler("anon"));
        methods.put(info("/secret"), handler("secured"));
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(methods);

        List<String> result = AnonymousScanner.scan(mapping);

        assertThat(result).containsExactly("/login");
    }

    @Test
    @DisplayName("一个方法的多个路径模板全部收集，且跨方法去重")
    void collectsAllPatternsAndDeduplicates() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/a", "/b"), handler("multiPath"));
        methods.put(info("/a"), handler("anon")); // 与上面重复的 /a
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(methods);

        List<String> result = AnonymousScanner.scan(mapping);

        assertThat(result).containsExactlyInAnyOrder("/a", "/b");
    }

    @Test
    @DisplayName("无 @Anonymous 方法时返回空列表")
    void emptyWhenNoAnonymous() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/secret"), handler("secured"));
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(methods);

        assertThat(AnonymousScanner.scan(mapping)).isEmpty();
    }
}
