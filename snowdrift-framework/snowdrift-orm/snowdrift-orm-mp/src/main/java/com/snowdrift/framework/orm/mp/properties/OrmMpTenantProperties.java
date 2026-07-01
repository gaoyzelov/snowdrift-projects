package com.snowdrift.framework.orm.mp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.Set;

/**
 * 多租户配置属性
 * <p>
 * 配置前缀：{@code snowdrift.orm.mp.tenant}<br>
 * 启用后，所有 SQL 自动追加租户过滤条件（除 ignoreTables 中配置的表外）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:27
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.orm.mp.tenant")
public class OrmMpTenantProperties implements Serializable {

    /**
     * 是否启用多租户 SQL 拦截（默认关闭）
     */
    private Boolean enabled;

    /**
     * 不需要租户过滤的表名集合<br>
     * 例如系统配置表、字典表等全局共享表
     */
    private Set<String> ignoreTables;
}
