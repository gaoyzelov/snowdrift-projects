package com.snowdrift.orm.mybatisplus.anno;

import com.snowdrift.orm.mybatisplus.enums.DesensitizeTypeEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Sensitive
 *
 * @author gaoye
 * @date 2025/03/20 14:25:22
 * @description 自定义脱敏注解
 * @since 1.0.0
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Desensitize {

    DesensitizeTypeEnum value() default DesensitizeTypeEnum.PASSWORD;

    @AliasFor("value")
    DesensitizeTypeEnum type();
}