package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.security.annotation.Anonymous;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link Anonymous} 注解端点扫描器
 * <p>
 * 启动时扫描所有 Controller 方法，收集标注了 {@link Anonymous} 的 URL 模板<b>及其 HTTP 方法条件</b>，
 * 供安全链按「路径 + 方法」精确放行。
 * </p>
 * <p>
 * 模板不做任何改写。{@code requestMatchers(String)} 在本模块中由 {@code MvcRequestMatcher} 承接，
 * 模式会交给 Spring MVC 自身的匹配引擎求值，因此原始模板是唯一能完整表达路径变量、正则约束
 * （{@code {id:[0-9]+}}）与 catch-all（{@code {*path}}）的形式；任何字符串改写都会改变放行范围。
 * </p>
 * <p>
 * <b>为什么必须带方法条件</b>：同一个 URL 模板上可以同时存在匿名与受保护的映射
 * （如 {@code @Anonymous @GetMapping("/thing")} 与 {@code @PostMapping("/thing")}），
 * 只按路径放行会把后者的方法一并放开。判定规则：
 * </p>
 * <ul>
 *   <li>声明了具体方法的 {@link Anonymous} 映射：这些方法进入放行集合（{@code GET} 同时覆盖
 *       Spring MVC 对 {@code HEAD} 的回退匹配）</li>
 *   <li>未声明方法的 {@link Anonymous} 映射：优先级最低，覆盖其余方法，但要扣掉同路径上
 *       已被更具体映射（无论是否匿名）承接的方法</li>
 * </ul>
 * <p>
 * <b>未建模的条件</b>：{@code params} / {@code headers} / {@code consumes} / {@code produces}。
 * 若同一路径上的映射仅靠这些条件区分优先级，本类仍可能放行到更具体的受保护映射 —— 这种写法请改用
 * {@code snowdrift.security.spring.exclude-path-patterns} 显式声明，或直接拆分路径。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public final class AnonymousScanner {

    /**
     * Spring MVC 可声明的全部 HTTP 方法。{@code GET} 映射在 Spring MVC 中同时承接 {@code HEAD} 请求，
     * 放行集合已按此口径展开。
     */
    public static final Set<HttpMethod> ALL_METHODS = Set.of(
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE);

    private AnonymousScanner() {
    }

    /**
     * 扫描所有标注 {@link Anonymous} 的方法，得到「URL 模板 → 可匿名访问的 HTTP 方法」列表
     *
     * @param handlerMapping RequestMappingHandlerMapping
     * @return 放行端点列表，{@link Endpoint#methods()} 恒非空
     */
    public static List<Endpoint> scan(RequestMappingHandlerMapping handlerMapping) {
        // 标注 @Anonymous 且声明了具体方法的映射：路径 -> 方法并集
        Map<String, Set<HttpMethod>> anonymousDeclared = new LinkedHashMap<>();
        // 该路径上所有映射（含受保护映射）声明过的具体方法
        Map<String, Set<HttpMethod>> occupiedMethods = new LinkedHashMap<>();
        // 存在未声明方法条件的 @Anonymous 映射的路径
        Set<String> anonymousUnbounded = new LinkedHashSet<>();

        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            Set<HttpMethod> declared = toHttpMethods(info.getMethodsCondition().getMethods());
            boolean anonymous = handler.hasMethodAnnotation(Anonymous.class);
            for (String pattern : info.getPatternValues()) {
                occupiedMethods.merge(pattern, declared, AnonymousScanner::union);
                if (!anonymous) {
                    continue;
                }
                if (declared.isEmpty()) {
                    anonymousUnbounded.add(pattern);
                } else {
                    anonymousDeclared.merge(pattern, declared, AnonymousScanner::union);
                }
            }
        });

        List<Endpoint> endpoints = new ArrayList<>(anonymousDeclared.size() + anonymousUnbounded.size());
        anonymousDeclared.forEach((pattern, methods) -> {
            if (!anonymousUnbounded.contains(pattern)) {
                endpoints.add(new Endpoint(pattern, methods));
            }
        });
        for (String pattern : anonymousUnbounded) {
            Set<HttpMethod> permitted = new LinkedHashSet<>(ALL_METHODS);
            // 未声明方法的映射优先级最低，Spring MVC 会把已被更具体映射承接的方法分派给后者
            permitted.removeAll(occupiedMethods.getOrDefault(pattern, Set.of()));
            permitted.addAll(anonymousDeclared.getOrDefault(pattern, Set.of()));
            if (!permitted.isEmpty()) {
                endpoints.add(new Endpoint(pattern, permitted));
            }
        }
        return List.copyOf(endpoints);
    }

    /**
     * 把映射声明的方法条件转成 {@link HttpMethod}；未声明（{@code @RequestMapping} 未限定 method）返回空集
     */
    private static Set<HttpMethod> toHttpMethods(Set<RequestMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            return Set.of();
        }
        Set<HttpMethod> result = new LinkedHashSet<>(methods.size());
        for (RequestMethod method : methods) {
            result.add(HttpMethod.valueOf(method.name()));
            if (method == RequestMethod.GET) {
                // Spring MVC 把 HEAD 请求回退到 GET 映射，放行范围需一并覆盖
                result.add(HttpMethod.HEAD);
            }
        }
        return result;
    }

    /**
     * 合并同一路径上多个映射声明的方法条件（空集表示该映射未声明方法条件，不参与合并）
     */
    private static Set<HttpMethod> union(Set<HttpMethod> left, Set<HttpMethod> right) {
        Set<HttpMethod> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return merged;
    }

    /**
     * 一个可匿名访问的端点
     *
     * @param pattern 原样的 Spring MVC 路径模板
     * @param methods 可匿名访问的 HTTP 方法集合，恒非空；等于 {@link #ALL_METHODS} 时表示该路径全方法放行
     */
    public record Endpoint(String pattern, Set<HttpMethod> methods) {

        public Endpoint {
            methods = Collections.unmodifiableSet(new LinkedHashSet<>(methods));
        }

        /**
         * 该路径的所有 HTTP 方法均在放行范围内（映射未声明方法条件）
         */
        public boolean allMethods() {
            return methods.containsAll(ALL_METHODS);
        }
    }
}
