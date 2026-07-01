package com.snowdrift.framework.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 重复提交防护注解
 * <p>
 * 在方法执行前将请求标识写入缓存，若在指定时间窗口内再次提交相同请求则拒绝，
 * 用于实现接口幂等性。
 * </p>
 *
 * <pre>
 * // 基于方法参数防重
 * &#064;RepeatSubmit(key = "#orderNo", interval = 5)
 * public Result&lt;Void&gt; createOrder(String orderNo) { ... }
 *
 * // 基于用户 + 业务防重
 * &#064;RepeatSubmit(key = "'user:submit:' + #userId", interval = 60)
 * public Result&lt;Void&gt; submit(Long userId) { ... }
 * </pre>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 请求标识 key，支持 SpEL 表达式
     */
    String key();

    /**
     * 防重时间间隔（秒），在间隔内重复提交会被拒绝
     */
    long interval() default 5;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复提交时的提示信息
     * <p>
     * 支持国际化 key（如 {@code "cache.repeat.submit"}）或直接文本，
     * 最终由全局异常拦截器通过 I18nUtil 统一解析。
     * </p>
     */
    String message() default "cache.repeat.submit";

    /**
     * 提示信息参数（配合 message i18n key 使用）
     */
    String[] args() default {};
}
