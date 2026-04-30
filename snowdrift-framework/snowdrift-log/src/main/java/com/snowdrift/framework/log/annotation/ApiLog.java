package com.snowdrift.framework.log.annotation;

import com.snowdrift.framework.common.enums.BizTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ApiLog
 * @author 83674
 * @date 2026/4/30-14:27
 * @description 接口访问日志注解
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /**
     * 是否记录日志
     */
    boolean enable() default true;

    /**
     * 是否保存参数
     */
    boolean saveArgs() default true;

    /**
     * 是否保存返回结果
     */
    boolean saveResult() default false;

    /**
     * 脱敏字段
     */
    String[] mask() default {};

    /**
     * 模块
     */
    String module() default "";

    /**
     * 日志摘要
     */
    String summary() default "";

    /**
     * 业务类型
     */
    BizTypeEnum bizType();
}
