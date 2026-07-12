package com.snowdrift.framework.orm.core.anno;

import java.lang.annotation.*;

/**
 * Encrypted
 * <p>
 * 基于 AES/GCM 的字段级透明加解密注解。
 * 标记在实体类的 String 字段上，入库时自动加密，出库时自动解密。
 * </p>
 * <p>
 * <b>注意：</b>加密字段<b>不支持</b>在 WHERE 子句中作为查询条件。
 * 因为数据库中存储的是密文，而 MyBatis 传入的是明文，两者无法匹配。
 * 如需按加密字段查询，请在应用层先查全量再过滤，或使用等值索引方案（如盲索引）。
 * </p>
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
