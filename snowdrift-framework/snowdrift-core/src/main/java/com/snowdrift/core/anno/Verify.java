package com.snowdrift.core.anno;

import java.lang.annotation.*;

/**
 * Verify
 *
 * @author gaoye
 * @date 2025/06/13 14:09:57
 * @description 校验注解
 * @since 1.0.0
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Verify {

    boolean throwEx() default true;
}