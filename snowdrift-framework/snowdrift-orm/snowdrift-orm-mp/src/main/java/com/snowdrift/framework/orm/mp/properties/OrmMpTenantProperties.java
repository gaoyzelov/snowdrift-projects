package com.snowdrift.framework.orm.mp.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.Set;

/**
 * OrmMpTenantProperties
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:27
 * @description 多租户配置属性
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.orm.mp.tenant")
public class OrmMpTenantProperties implements Serializable {

    /**
     * 是否启用多租户 SQL 拦截（默认关闭）
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * 租户字段
     */
    @NotBlank
    private String tenantIdColumn = "tenant_id";

    /**
     * 不需要租户过滤的表名集合<br>
     * 例如系统配置表、字典表等全局共享表
     * <p>表名匹配忽略大小写。</p>
     */
    private Set<String> ignoreTables;

    /**
     * 无租户上下文写入多租户实体时使用的默认租户ID
     * <p>不配置（{@code null}）时保持 fail-closed：写入 {@code TenantEntity} 但取不到租户上下文会直接抛出
     * {@code BizException}，与读取侧 {@code MultiTenantLineHandler} 的行为一致，避免数据被静默写入
     * 任何租户都查询不到的“孤岛”。</p>
     * <p>定时任务、MQ 消费者、数据迁移等系统写入场景可显式配置该值；配置后此类写入落到指定租户，
     * 并输出 WARN 日志以便审计。</p>
     */
    private Long defaultTenantId;
}
