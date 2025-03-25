package com.snowdrift.web.anno;

import com.snowdrift.core.enums.AccessTypeEnum;

import java.lang.annotation.*;

/**
 * SysLog
 *
 * @author gaoye
 * @date 2025/03/24 13:50:09
 * @description xxxxxxxx
 * @since 1.0.0
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessLog {

    /**
     * 模块
     */
    String module();

    /**
     * 描述
     */
    String value();

    /**
     * 访问类型
     */
    AccessTypeEnum type() default AccessTypeEnum.GENERAL;

    /**
     * 脱敏字段
     */
    String[] mask() default {};
}