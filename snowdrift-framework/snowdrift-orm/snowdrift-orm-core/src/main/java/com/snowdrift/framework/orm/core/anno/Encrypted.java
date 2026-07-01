package com.snowdrift.framework.orm.core.anno;

import java.lang.annotation.*;

/**
 * 字段加解密注解
 * <p>
 * 标注在实体类字段上,实现：
 * <ul>
 *   <li>写入数据库前自动 AES 加密（密文前缀 <code>{ENC}</code>）</li>
 *   <li>读取数据库后自动 AES 解密（检测 <code>{ENC}</code> 前缀后移除并解密）</li>
 * </ul>
 * 支持 {@link String} 类型字段，已加密的值不会重复加密。
 * </p>
 *
 * <pre>{@code
 * public class User extends BaseEntity {
 *     @Encrypted
 *     private String phone;  // 入库自动加密，出库自动解密
 * }
 * }</pre>
 *
 * @author 83674
 * @date 2026/7/1-15:57
 * @since 1.0.0
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
}
