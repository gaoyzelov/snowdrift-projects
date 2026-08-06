package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * TenantBaseEntity
 *
 * @author gaoyzelov
 * @date 2026/7/1-16:02
 * @description 多租户实体基类
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantBaseEntity extends BaseEntity {

    /**
     * 租户ID（INSERT 时自动从填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
