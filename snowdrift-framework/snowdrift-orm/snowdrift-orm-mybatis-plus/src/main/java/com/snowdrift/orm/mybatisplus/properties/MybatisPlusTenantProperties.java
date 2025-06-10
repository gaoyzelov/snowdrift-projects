package com.snowdrift.orm.mybatisplus.properties;

import com.snowdrift.core.constant.ColumnConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.Set;

/**
 * TenantProperties
 *
 * @author gaoye
 * @date 2025/03/21 10:35:53
 * @description 多租户配置
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "mybatis-plus.tenant")
public class MybatisPlusTenantProperties implements Serializable {

    @NotNull(message = "租户功能是否启用不能为空")
    private Boolean enabled = Boolean.FALSE;

    @NotBlank(message = "租户ID字段名不能为空")
    private String tenantIdColumn = ColumnConst.TENANT_ID;

    @NotBlank(message = "默认租户ID不能为空")
    private String defaultTenantId = "0";

    private Set<String> ignoreTables;
}