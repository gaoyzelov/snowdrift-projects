package com.snowdrift.framework.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LoginLog
 * @author 83674
 * @date 2026/4/30-14:28
 * @description 登录日志注解
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginLog {

    /**
     * 是否记录日志
     */
    boolean enable() default true;
}
