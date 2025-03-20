package com.snowdrift.cache.redis.anno;

import java.lang.annotation.*;

/**
 * DistLock
 *
 * @author gaoye
 * @date 2025/03/20 09:35:19
 * @description 分布式锁注解
 * @since 1.0.0
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistLock {

    /**
     * 锁前缀
     */
    String value() default "redisson";

    /**
     * 锁key
     */
    String key() default "";

    /**
     * 锁过期时间，单位秒
     */
    int lockTime() default 10;

    /**
     * 获取锁超时时间，单位秒
     */
    int waitTime() default 0;

    /**
     * 是否阻塞获取
     */
    boolean block() default false;

    /**
     * 提示信息
     */
    String msg() default "操作频繁，请稍后再试!";
}