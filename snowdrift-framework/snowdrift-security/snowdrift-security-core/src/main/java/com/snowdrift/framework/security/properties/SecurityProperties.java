package com.snowdrift.framework.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Security 模块统一的公共配置（基类）
 * <p>
 * 仅定义 Sa-Token 与 Spring Security 通用的基础配置项。
 * 各实现模块通过继承此类扩展特有配置（如 Token 过期策略、并发登录控制等），
 * 并在子类上添加 {@code @ConfigurationProperties} 绑定对应前缀。
 * </p>
 *
 * @author 83674
 * @date 2026/5/15
 * @since 1.0.0
 */
@Data
public class SecurityProperties {

    /**
     * 是否启用安全模块
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Token 所在的请求头名称
     */
    @NotBlank
    private String headerName = "Authorization";

    /**
     * Token 过期时间（秒），默认 24 小时
     */
    @NotNull
    private Long timeout = 86400L;

    /**
     * Token 活跃超时时间（秒）
     * <p>
     * 用户最后一次访问后，Token 保持活跃的最大时长。
     * 超过此时间未访问则视为闲置，Token 将被清理。
     * 默认 1800 秒（30 分钟）。
     * </p>
     */
    @NotNull
    private long activeTimeout = 1800;

    /**
     * Token 前缀（拼接在 Token 值之前，中间用空格分隔）
     * <p>
     * 如 Bearer Token 格式：{@code Authorization: Bearer <token>}
     * </p>
     */
    private String prefix = "Bearer";

    /**
     * 拦截器排除的路径列表（Ant 风格通配符）
     * <p>
     * 这些路径不会进行登录校验，通常包括静态资源、API 文档、错误页面等。
     * 各实现模块可直接读取此配置，无需重复定义。
     * </p>
     */
    @NotEmpty
    private List<String> excludePathPatterns = List.of(
            "/favicon.ico",
            "/swagger-resources/**",
            "/v2/api-docs/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/swagger-ui.html",
            "/error"
    );
}
