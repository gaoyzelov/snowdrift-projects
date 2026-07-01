package com.snowdrift.framework.security.spring.auth;

import com.snowdrift.framework.security.annotation.AnonymousAccess;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * @AnonymousAccess 注解路径扫描器
 * <p>
 * 启动时扫描所有 Controller 方法，收集标注了 {@link AnonymousAccess} 的方法 URL 模式，
 * 转换为 Ant 风格路径后注入到 Spring Security 的放行列表中。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public final class AnonymousAccessScanner {

    private AnonymousAccessScanner() {
    }

    /**
     * 扫描所有标注 @AnonymousAccess 的方法 URL 路径
     *
     * @param handlerMapping RequestMappingHandlerMapping
     * @return Ant 风格的路径列表
     */
    public static List<String> scan(RequestMappingHandlerMapping handlerMapping) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        return handlerMethods.entrySet().stream()
                .filter(e -> e.getValue().hasMethodAnnotation(AnonymousAccess.class))
                .flatMap(e -> e.getKey().getPatternValues().stream())
                .map(AnonymousAccessScanner::toAntPattern)
                .distinct()
                .toList();
    }

    /**
     * 将 Spring MVC URI 模板转为 Ant 风格（{id} → *）
     */
    private static String toAntPattern(String mvcPattern) {
        return mvcPattern.replaceAll("\\{[^}]+\\}", "*");
    }
}
