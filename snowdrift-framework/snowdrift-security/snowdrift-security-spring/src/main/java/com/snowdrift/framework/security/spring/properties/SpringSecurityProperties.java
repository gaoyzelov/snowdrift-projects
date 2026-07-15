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
     * 是否启用 CSRF 防护
     * REST API 默认关闭
     */
    @NotNull
    private boolean csrfEnabled = false;

    /**
     * 是否启用 CORS 跨域支持
     */
    @NotNull
    private boolean corsEnabled = true;
}
