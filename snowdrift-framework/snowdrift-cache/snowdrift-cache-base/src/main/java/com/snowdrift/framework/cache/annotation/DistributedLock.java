package com.snowdrift.framework.cache.annotation;

import com.snowdrift.framework.cache.IDistributedLockService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * <p>
 * 通过 AOP 在方法执行前自动获取分布式锁，执行后自动释放。
 * 需要类路径中存在 {@link IDistributedLockService} 的实现（如 Redisson 模块）。
 * </p>
 *
 * <pre>
 * // 简单用法：固定 key
 * &#064;DistributedLock(key = "order:pay")
 *
 * // SpEL 表达式：动态 key
 * &#064;DistributedLock(key = "'order:pay:' + #orderId", waitTime = 3, leaseTime = 10)
 *
 * // 自定义失败消息
 * &#064;DistributedLock(key = "#id", message = "订单处理中，请稍后重试")
 * </pre>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式
     */
    String key();

    /**
     * 获取锁失败时的提示信息
     */
    String message() default "请求过于频繁，请稍后重试！";

    /**
     * 获取锁的等待时间（秒），0 表示不等待，获取失败直接抛异常
     */
    long waitTime() default 0;

    /**
     * 持有锁的时间（秒），-1 表示使用看门狗自动续期
     */
    long leaseTime() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
