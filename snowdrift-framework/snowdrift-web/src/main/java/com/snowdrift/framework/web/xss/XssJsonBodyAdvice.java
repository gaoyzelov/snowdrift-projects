package com.snowdrift.framework.web.xss;

import com.snowdrift.framework.web.properties.XssProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;

/**
 * XSS JSON Body 清洗 Advice
 * <p>
 * 在 Spring MVC 层对 {@code @RequestBody} 反序列化后的对象递归清洗 String 值，
 * 弥补 {@link com.snowdrift.framework.web.filter.XssFilter} 仅覆盖参数/Header
 * 而不覆盖 JSON Body 的防护盲区。
 * </p>
 *
 * <h3>清洗范围</h3>
 * <ul>
 *   <li>{@link String} — 直接调用 {@link XssCleaner#clean(String)} 清洗</li>
 *   <li>{@link Map} — 遍历 value，对 String 值递归清洗（不可变 Map 跳过）</li>
 *   <li>{@link Collection} — 遍历元素递归清洗</li>
 *   <li>数组 — 遍历元素递归清洗</li>
 *   <li>复杂 POJO — 不做深度反射（安全边界）</li>
 * </ul>
 *
 * <h3>安全防护</h3>
 * <ul>
 *   <li>通过 {@link IdentityHashMap} 检测循环引用，防止无限递归</li>
 *   <li>不可变集合 / 不可变 Map 静默跳过（捕获 UnsupportedOperationException）</li>
 *   <li>通过 {@link XssProperties#getExcludePathPatterns()} 排除富文本等路径</li>
 * </ul>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
@ControllerAdvice
public class XssJsonBodyAdvice implements RequestBodyAdvice {

    private static final Logger log = LoggerFactory.getLogger(XssJsonBodyAdvice.class);

    private final XssProperties xssProperties;
    private final XssCleaner xssCleaner;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssJsonBodyAdvice(XssProperties xssProperties, XssCleaner xssCleaner) {
        this.xssProperties = xssProperties;
        this.xssCleaner = xssCleaner;
    }

    // =================== RequestBodyAdvice 实现 ===================

    /**
     * 判断是否需要清洗当前请求的 Body
     * <p>
     * XSS 未启用或当前路径在排除列表中时返回 false，跳过清洗。
     * </p>
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (Boolean.FALSE.equals(xssProperties.getEnabled())) {
            return false;
        }
        // 检查路径排除
        List<String> excludes = xssProperties.getExcludePathPatterns();
        if (CollectionUtils.isNotEmpty(excludes)) {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String path = servletAttrs.getRequest().getServletPath();
                if (excludes.stream().anyMatch(p -> pathMatcher.match(p, path))) {
                    log.debug("XSS JSON Body 清洗已跳过（排除路径）: path={}", path);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (body == null) {
            return null;
        }
        return cleanObject(body, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    // =================== 递归清洗 ===================

    /**
     * 递归清洗对象树中的 String 值
     *
     * @param obj     待清洗对象
     * @param visited 已访问对象引用（用于循环引用检测）
     * @return 清洗后的对象（String 被替换为清洗后的值，Map/Collection 原地修改）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object cleanObject(Object obj, Set<Object> visited) {
        if (obj == null) {
            return null;
        }

        // 循环引用检测
        if (visited.contains(obj)) {
            return obj;
        }
        visited.add(obj);

        // String → 直接清洗
        if (obj instanceof String str) {
            return xssCleaner.clean(str);
        }

        // Map → 遍历 value 递归清洗
        if (obj instanceof Map<?, ?> map) {
            cleanMap((Map) map, visited);
            return obj;
        }

        // Collection → 遍历元素递归清洗
        if (obj instanceof Collection<?> collection) {
            cleanCollection(collection, visited);
            return obj;
        }

        // 数组 → 遍历递归清洗
        if (obj.getClass().isArray()) {
            cleanArray(obj, visited);
            return obj;
        }

        // 其他类型（原始类型、Date、Enum、复杂 POJO 等）→ 跳过
        return obj;
    }

    /**
     * 清洗 Map 中所有 String 类型的 value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cleanMap(Map map, Set<Object> visited) {
        for (Object entryObj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            Object originalValue = entry.getValue();
            Object cleanedValue = cleanObject(originalValue, new HashSet<>(visited));
            if (cleanedValue != originalValue) {
                try {
                    map.put(entry.getKey(), cleanedValue);
                } catch (UnsupportedOperationException e) {
                    log.trace("XSS 清洗跳过不可变 Map: key={}", entry.getKey());
                }
            }
        }
    }

    /**
     * 清洗 Collection 中所有 String 类型的元素
     */
    private void cleanCollection(Collection<?> collection, Set<Object> visited) {
        for (Object item : collection) {
            cleanObject(item, new HashSet<>(visited));
        }
    }

    /**
     * 清洗数组中所有 String 类型的元素
     */
    private void cleanArray(Object array, Set<Object> visited) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            Object cleaned = cleanObject(item, new HashSet<>(visited));
            if (cleaned != item) {
                try {
                    Array.set(array, i, cleaned);
                } catch (Exception e) {
                    log.trace("XSS 清洗跳过不可变数组元素: index={}", i);
                }
            }
        }
    }
}
