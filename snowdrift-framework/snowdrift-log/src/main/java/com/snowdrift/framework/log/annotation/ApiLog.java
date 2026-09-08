package com.snowdrift.framework.log.annotation;

import com.snowdrift.framework.base.enums.BizTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ApiLog
 * @author gaoyzelov
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
     * 是否保存请求参数。
     * <p>
     * 默认关闭，避免敏感参数（密码、Token 等）被默认写入日志；
     * 需要记录时可设为 {@code true}，并配合 {@code mask()} 对敏感字段脱敏。
     * </p>
     */
    boolean saveParams() default false;

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
