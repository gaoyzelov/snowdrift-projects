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
    private Boolean enabled = false;

    /**
     * 租户字段
     */
    @NotBlank
    private String tenantIdColumn = "tenant_id";

    /**
     * 不需要租户过滤的表名集合<br>
     * 例如系统配置表、字典表等全局共享表
     */
    private Set<String> ignoreTables;
}
