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
 * @author gaoyzelov
 * @date 2026/5/15
 * @since 1.0.0
 */
@Data
public class SecurityProperties {

    /**
     * 是否启用安全模块
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * Token 所在的请求头名称
     */
    @NotBlank
    private String headerName = "Authorization";

    /**
     * Token 过期时间（秒），默认 24 小时
     * <p>
     * 小于等于 0 表示永不过期（对齐 Sa-Token 的 -1 约定）。
     * </p>
     */
    @NotNull
    private Long timeout = 86400L;

    /**
     * Token 活跃超时时间（秒）
     * <p>
     * 用户最后一次访问后，Token 保持活跃的最大时长。
     * 超过此时间未访问则视为闲置，Token 将被清理。
     * 默认 1800 秒（30 分钟）。
     * 小于等于 0 表示不限制闲置时长，即不做闲置淘汰。
     * </p>
     */
    @NotNull
    private Long activeTimeout = 1800L;

    /**
     * Token 前缀（拼接在 Token 值之前，中间用空格分隔）
     * <p>
     * 如 Bearer Token 格式：{@code Authorization: Bearer <token>}
     * </p>
     */
    private String prefix = "Bearer";

    /**
     * 拦截器排除的路径列表
     * <p>
     * 这些路径不会进行登录校验，通常包括静态资源、错误页面等。
     * 采用 Spring MVC 的路径模板语义（{@code PathPattern}，如 {@code /doc/**}、{@code /api/{id}}），
     * 安全链、上下文过滤器与 Sa-Token 拦截器共用同一份配置，语义保持一致。
     * </p>
     * <p>
     * <b>默认不包含接口文档路径</b>（{@code /v3/api-docs/**}、{@code /doc.html}、{@code /swagger-ui.html}、
     * {@code /swagger-resources/**}、{@code /v2/api-docs/**}）：这些路径会把全部接口定义与参数结构暴露出去，
     * 是否对外可见应由使用方按环境决定，不宜由框架默认放行。生产环境请保持默认；开发/联调环境需要时自行加上。
     * </p>
     * <p>
     * 该属性为<b>整份替换</b>语义：一旦配置，默认值全部失效，需要把 {@code /error} 等一并写回，例如
     * {@code snowdrift.security.exclude-path-patterns=/favicon.ico,/error,/v3/api-docs/**,/doc.html}。
     * </p>
     */
    @NotEmpty
    private List<String> excludePathPatterns = List.of(
            "/favicon.ico",
            "/error"
    );
}
