package com.snowdrift.framework.orm.core.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DataScope 数据权限注解
 * <p><b>适用范围：</b>仅对 <b>MyBatis Mapper 接口及其方法</b>生效。处理器通过语句 ID
 * （{@code <namespace>.<statementId>}）反查类与方法，因此标注在 Service、Controller 等
 * 非 Mapper 类型上不会产生任何效果。</p>
 * <p>标注在类上时对该 Mapper 的所有语句生效；方法上的标注优先于类上的标注。
 * 同名重载方法若挂载了不同的注解，将在执行时抛出异常，而不会随机命中其中之一。</p>
 *
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
