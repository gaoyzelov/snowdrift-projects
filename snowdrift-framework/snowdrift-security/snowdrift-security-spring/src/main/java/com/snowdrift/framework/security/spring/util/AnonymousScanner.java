package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.security.annotation.Anonymous;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * {@link Anonymous} 注解路径扫描器
 * <p>
 * 启动时扫描所有 Controller 方法，收集标注了 {@link Anonymous} 的 URL 模板，注入到 Spring Security 的放行列表。
 * </p>
 * <p>
 * 模板不做任何改写。{@code requestMatchers(String)} 在本模块中由 {@code MvcRequestMatcher} 承接，
 * 模式会交给 Spring MVC 自身的匹配引擎求值，因此原始模板是唯一能完整表达路径变量、正则约束
 * （{@code {id:[0-9]+}}）与 catch-all（{@code {*path}}）的形式；任何字符串改写都会改变放行范围。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public final class AnonymousScanner {

    private AnonymousScanner() {
    }

    /**
     * 扫描所有标注 {@link Anonymous} 的方法 URL 模板
     *
     * @param handlerMapping RequestMappingHandlerMapping
     * @return Spring MVC 路径模板列表（未做改写）
     */
    public static List<String> scan(RequestMappingHandlerMapping handlerMapping) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        return handlerMethods.entrySet().stream()
                .filter(e -> e.getValue().hasMethodAnnotation(Anonymous.class))
                .flatMap(e -> e.getKey().getPatternValues().stream())
                .distinct()
                .toList();
    }
}
