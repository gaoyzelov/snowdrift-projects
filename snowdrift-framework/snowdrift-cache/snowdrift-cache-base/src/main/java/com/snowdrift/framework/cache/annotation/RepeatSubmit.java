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
 * <p><b>后端要求：</b>防重依赖 per-key 过期能力，Redis 与 Caffeine 后端都支持
 * （Caffeine 通过 {@code VarExpiration} 实现）。注意 Caffeine 是进程内缓存，
 * 多实例部署时防重只在单个 JVM 内生效，跨实例防重请使用 Redis。</p>
 * <p><b>{@link #interval()} 必须大于 0</b>：为 0 时标记会立即过期、防重窗口为空，
 * 所有重复请求都会被放行，因此该配置会在切面中直接抛 {@code BizException}。</p>
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
     */
    String message() default "请勿重复提交，请稍后重试";
}
