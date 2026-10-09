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
 * 在 Spring MVC 层对 {@code @RequestBody} <b>反序列化后</b>的对象做递归清洗。需要明确它<b>覆盖面很窄</b>。
 * </p>
 *
 * <h3>清洗范围（以及明确不做的事）</h3>
 * <ul>
 *   <li>{@link String} — 调用 {@link XssCleaner#clean(String)} 清洗</li>
 *   <li>{@link Map} — <b>只清洗 value，key 不清洗</b>；不可变 Map 跳过</li>
 *   <li>{@link List} — 逐元素原地替换；其他 {@link Collection}（如 {@link java.util.Set}）
 *       只递归其内部结构，<b>String 元素本身不会被清洗</b></li>
 *   <li>数组 — 逐元素原地替换</li>
 *   <li><b>复杂 POJO — 整体跳过（不做反射）</b></li>
 * </ul>
 * <p>
 * 因此对最常见的 {@code @RequestBody MyDto}，本 Advice <b>不产生任何清洗效果</b>：
 * {@link com.snowdrift.framework.web.filter.XssFilter} 只覆盖请求参数与 Header，
 * JSON Body 中 DTO 的字段不在两者的防护范围内。
 * </p>
 *
 * <h3>关于所用清洗器</h3>
 * <p>
 * 默认注入的 {@link SimpleXssCleaner} 做的是 HTML 实体转义，其自身文档即指出"<b>不适用于 JSON 输出</b>"：
 * 转义会改变载荷内容，对已转义内容再次清洗还会产生双重编码。故本 Advice 的定位是
 * "<b>仅在 body 为 Map/集合/数组形态时的兜底</b>"，不应被当作 JSON 接口的 XSS 防线——
 * XSS 属输出上下文问题，应在渲染/响应阶段按上下文编码，而不是在入口改写业务数据。
 * </p>
 *
 * <h3>其他</h3>
 * <ul>
 *   <li>整体开关见 {@link XssProperties#getEnabled()}，<b>默认关闭</b></li>
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
     * 清洗 Map 中所有 String 类型的 value（key 不处理）
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
     * 清洗 Collection 中的 String 元素（<b>仅 List 支持原地替换</b>）
     * <p>
     * List 类型通过 {@link ListIterator#set(Object)} 原地替换清洗后的值；
     * 其他 Collection（如 Set）无法安全替换元素，仅递归处理其内部的 Map/Collection 结构，
     * 因此 {@code Set<String>} 中的字符串不会被清洗。
     * </p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cleanCollection(Collection collection, Set<Object> visited) {
        if (collection instanceof List list) {
            ListIterator<Object> it = list.listIterator();
            while (it.hasNext()) {
                Object item = it.next();
                Object cleaned = cleanObject(item, new HashSet<>(visited));
                if (cleaned != item) {
                    try {
                        it.set(cleaned);
                    } catch (UnsupportedOperationException e) {
                        log.trace("XSS 清洗跳过不可变 List 元素: index={}", it.previousIndex());
                    }
                }
            }
        } else {
            // Set 等无序集合无法安全替换元素，仅递归处理内部结构
            for (Object item : collection) {
                cleanObject(item, new HashSet<>(visited));
            }
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
