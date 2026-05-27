package com.snowdrift.framework.security.satoken.properties;

import com.snowdrift.framework.security.properties.SecurityProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sa-Token 安全模块扩展配置
 * <p>
 * 继承 {@link SecurityProperties} 的公共配置项，并追加 Sa-Token 特有的
 * Token 过期策略、并发登录控制等配置。通过 Spring Boot 的
 * {@link ConfigurationProperties} 机制绑定 {@code snowdrift.security} 前缀。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "snowdrift.security.sa-token")
public class SaTokenSecurityProperties extends SecurityProperties {

    /**
     * Token 活跃超时时间（秒）
     * <p>
     * 用户最后一次访问后，Token 保持活跃的最大时长。
     * 超过此时间未访问则 Token 冻结，默认 30 分钟。
     * </p>
     */
    @NotNull
    private long activeTimeout = 1800;

    /**
     * 是否允许多端同时登录
     * <ul>
     *   <li>{@code true} — 同一用户可在多个设备同时在线</li>
     *   <li>{@code false} — 新登录会踢出旧会话（顶号）</li>
     * </ul>
     */
    @NotNull
    private boolean concurrent = true;

    /**
     * 多人登录同一账号时，是否共用一个 Token
     * <p>
     * {@code false} 时每次登录生成新 Token，配合 {@link #maxLoginCount} 控制最大登录数。
     * </p>
     */
    @NotNull
    private boolean isShare = false;

    /**
     * 同一账号最大登录数量
     * 仅在 {@link #isShare} 为 {@code false} 时生效
     */
    @Min(1)
    private int maxLoginCount = 12;

    /**
     * Token 生成风格
     * <ul>
     *   <li>uuid — 标准 UUID（默认）</li>
     *   <li>simple-uuid — 无中划线 UUID</li>
     *   <li>random-32/64/128 — 随机字符串</li>
     *   <li>tik — 时序有序，适合数据库存储和索引</li>
     * </ul>
     */
    @NotNull
    private String tokenStyle = "uuid";

    /**
     * 是否输出 Sa-Token 框架自身操作日志
     * 调试阶段可开启，生产环境建议关闭
     */
    @NotNull
    private boolean isLog = false;
}
