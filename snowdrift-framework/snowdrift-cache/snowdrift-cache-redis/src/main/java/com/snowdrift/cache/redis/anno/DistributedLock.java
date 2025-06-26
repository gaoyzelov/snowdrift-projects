package com.snowdrift.cache.redis.anno;

import java.lang.annotation.*;

/**
 * DistributedLock
 *
 * @author gaoye
 * @date 2025/06/26 13:56:26
 * @description xxxxxxxx
 * @since 1.0
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String value() default "redisson";

    int leaseTime() default 5;

    int waitTime() default 0;

    boolean block() default true;

    String failMsg() default "操作过于频繁，请稍后再试!";
}