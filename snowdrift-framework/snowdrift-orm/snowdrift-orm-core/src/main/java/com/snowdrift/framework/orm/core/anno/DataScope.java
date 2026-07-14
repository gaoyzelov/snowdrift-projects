package com.snowdrift.framework.orm.core.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DataScope
 *
 * @author gaoyzelov
 * @date 2026/7/2-14:14
 * @description 数据权限注解
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * 主表别名，多表联查时使用
     */
    String alias() default "";

    /**
     * 部门字段名
     *
     */
    String deptColumn() default "dept_id";

    /**
     * 创建人字段
     */
    String userColumn() default "user_id";
}
