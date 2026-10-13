package com.snowdrift.framework.security.spring.properties;

import com.snowdrift.framework.security.properties.SecurityProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Security 模块扩展配置
 * <p>
 * 继承 {@link SecurityProperties} 的公共配置项，通过
 * {@link ConfigurationProperties} 绑定 {@code snowdrift.security.spring} 前缀。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Data
@Validated
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "snowdrift.security.spring")
public class SpringSecurityProperties extends SecurityProperties {

    /**
     * Token 存储模式。默认使用 Redis，避免多实例部署时登录态只保存在单个进程内。
     */
    @NotNull
    private TokenStoreMode tokenStoreMode = TokenStoreMode.REDIS;

    /**
     * 是否启用 CSRF 防护
     * REST API 默认关闭
     */
    @NotNull
    private Boolean csrfEnabled = Boolean.FALSE;

    /**
     * 是否启用 CORS 跨域支持
     */
    @NotNull
    private Boolean corsEnabled = Boolean.FALSE;

    /**
     * Spring Security TokenStore 存储模式。
     */
    public enum TokenStoreMode {
        /** Redis 不可用时启动失败；适用于共享登录态的常规部署。 */
        REDIS,
        /** 显式进程内存储；仅适用于开发或单实例部署。 */
        MEMORY
    }
}
