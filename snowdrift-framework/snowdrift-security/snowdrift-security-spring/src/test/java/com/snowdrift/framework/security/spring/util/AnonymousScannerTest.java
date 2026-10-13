package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.security.annotation.Anonymous;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AnonymousScannerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description @Anonymous 端点扫描黑盒测试：收集标注方法的原始模板、保留映射的 HTTP 方法条件、
 *              同路径多映射的并集与「更具体映射优先」扣减、未标注方法不放行
 * @since 1.0.0
 */
class AnonymousScannerTest {

    private static final Set<HttpMethod> ALL = AnonymousScanner.ALL_METHODS;
    private static final Set<HttpMethod> GET_WITH_HEAD = Set.of(HttpMethod.GET, HttpMethod.HEAD);

    static class TestController {
        @Anonymous
        public void anon() {
        }

        public void secured() {
        }
    }

    private static RequestMappingInfo info(Set<RequestMethod> methods, String... patterns) {
        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
        RequestMappingInfo.Builder builder = RequestMappingInfo.paths(patterns).options(options);
        if (!methods.isEmpty()) {
            builder.methods(methods.toArray(new RequestMethod[0]));
        }
        return builder.build();
    }

    private static RequestMappingInfo info(String... patterns) {
        return info(Set.of(), patterns);
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    private static List<AnonymousScanner.Endpoint> scan(Map<RequestMappingInfo, HandlerMethod> methods) {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(methods);
        return AnonymousScanner.scan(mapping);
    }

    @Test
    @DisplayName("仅收集标注 @Anonymous 方法的 URL 模板，未标注方法被忽略")
    void scansOnlyAnonymousMethods() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/login"), handler("anon"));
        methods.put(info("/secret"), handler("secured"));

        assertThat(scan(methods)).containsExactly(new AnonymousScanner.Endpoint("/login", ALL));
    }

    @Test
    @DisplayName("一个方法的多个路径模板全部收集，且同一路径不重复出现")
    void collectsAllPatternsAndDeduplicates() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/a", "/b"), handler("anon"));
        methods.put(info("/a"), handler("anon"));

        assertThat(scan(methods)).containsExactlyInAnyOrder(
                new AnonymousScanner.Endpoint("/a", ALL),
                new AnonymousScanner.Endpoint("/b", ALL));
    }

    @Test
    @DisplayName("无 @Anonymous 方法时返回空列表")
    void emptyWhenNoAnonymous() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/secret"), handler("secured"));

        assertThat(scan(methods)).isEmpty();
    }

    @Test
    @DisplayName("保留映射声明的 HTTP 方法条件：GET 映射不得放行 POST（GET 同时覆盖 HEAD 回退）")
    void keepsDeclaredMethods() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info(Set.of(RequestMethod.GET), "/thing"), handler("anon"));

        assertThat(scan(methods)).containsExactly(
                new AnonymousScanner.Endpoint("/thing", GET_WITH_HEAD));
    }

    @Test
    @DisplayName("同一路径上未被标注 @Anonymous 的方法不进入放行集合")
    void onlyAnonymousMethodsOfSamePathAreCollected() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info(Set.of(RequestMethod.GET), "/thing"), handler("anon"));
        methods.put(info(Set.of(RequestMethod.POST), "/thing"), handler("secured"));

        assertThat(scan(methods)).containsExactly(
                new AnonymousScanner.Endpoint("/thing", GET_WITH_HEAD));
    }

    @Test
    @DisplayName("同一路径上多个 @Anonymous 映射的方法条件取并集")
    void unionsMethodsOfSamePath() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info(Set.of(RequestMethod.GET), "/thing"), handler("anon"));
        methods.put(info(Set.of(RequestMethod.POST), "/thing"), handler("anon"));

        assertThat(scan(methods)).containsExactly(
                new AnonymousScanner.Endpoint("/thing", Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST)));
    }

    @Test
    @DisplayName("未声明方法条件的 @Anonymous 映射覆盖其余所有方法")
    void undeclaredMethodsCoverTheRest() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info(Set.of(RequestMethod.GET), "/thing"), handler("anon"));
        methods.put(info("/thing"), handler("anon"));

        assertThat(scan(methods)).containsExactly(new AnonymousScanner.Endpoint("/thing", ALL));
    }

    @Test
    @DisplayName("更具体的受保护映射优先：未声明方法的 @Anonymous 映射不得放行已被它占用的 GET/HEAD")
    void undeclaredAnonymousMappingDoesNotShadowSecuredMapping() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(info("/thing"), handler("anon"));
        methods.put(info(Set.of(RequestMethod.GET), "/thing"), handler("secured"));

        assertThat(scan(methods)).containsExactly(new AnonymousScanner.Endpoint("/thing",
                Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
                        HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE)));
    }
}
