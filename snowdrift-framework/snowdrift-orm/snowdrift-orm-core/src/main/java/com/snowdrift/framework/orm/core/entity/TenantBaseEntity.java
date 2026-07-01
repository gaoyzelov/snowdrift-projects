package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

/**
 * 多租户实体基类
 * <p>
 * 继承自 {@link BaseEntity}，额外增加租户隔离能力：
 * 多租户业务实体继承此类；非多租户业务直接继承 {@link BaseEntity}。
 * </p>
 *
 * @author 83674
 * @date 2026/7/1-16:02
 * @since 1.0.0
 */
@Getter
@Setter
public class TenantBaseEntity extends BaseEntity {

    /**
     * 租户ID（INSERT 时自动从填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
