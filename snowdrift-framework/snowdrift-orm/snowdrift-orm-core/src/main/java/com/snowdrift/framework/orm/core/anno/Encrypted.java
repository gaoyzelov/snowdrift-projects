package com.snowdrift.framework.orm.core.anno;

import java.lang.annotation.*;

/**
 * Encrypted
 *
 * @author 83674
 * @date 2026/7/1-15:57
 * @description 字段加解密注解，基于AES
 * @since 1.0.0
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
}
