package com.snowdrift.framework.orm.mp.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

/**
 * OrmMpBaseProperties
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:26
 * @description 数据加解密配置属性
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.orm.mp")
public class OrmMpBaseProperties implements Serializable {

    /**
     * 是否启用乐观锁（默认关闭）
     */
    @NotNull
    private Boolean optimisticLock = Boolean.FALSE;

    /**
     * 是否显式允许业务自定义的 MybatisPlusInterceptor 完整替换框架默认实例。
     * 默认禁止，以免多租户、数据权限、防全表操作和分页等保护被无意整体绕过。
     */
    @NotNull
    private Boolean allowInterceptorOverride = Boolean.FALSE;

    /**
     * 是否启用字段加解密能力（默认关闭）
     * <p><b>本开关只负责校验并初始化密钥，不会自动加密任何字段。</b>指定哪些字段需要加解密由使用者在实体上标注，
     * 完整用法：</p>
     * <pre>{@code
     * @TableName(autoResultMap = true)          // 不开启时查询结果不会应用字段的 TypeHandler，读到的仍是密文
     * public class User {
     *     @TableField(typeHandler = AesEncryptTypeHandler.class)
     *     private String idCard;
     * }
     * }</pre>
     * <p>注意：加解密仅作用于 ORM 字段读写映射，WHERE / ORDER BY / 唯一性约束等 SQL 片段不会
     * 经过 TypeHandler，请勿将加密列用于过滤、排序或唯一键（详见 {@code AesEncryptTypeHandler}）。</p>
     */
    @NotNull
    private Boolean crypto = Boolean.FALSE;

    /**
     * AES 加密密钥（十六进制字符串，16/24/32 字节对应 AES-128/192/256）
     * <p>本项在配置文件中是明文，建议先用 Jasypt 等方案加密后再写入（同时须保护好加密口令本身）。</p>
     */
    private String cryptoKey;
}
