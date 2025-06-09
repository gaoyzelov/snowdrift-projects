package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户实体类
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/6 16:43
 */
@Getter
@Setter
@Schema(description = "TenantEntity")
public class TenantEntity extends BaseEntity {

    @Schema(description = "租户ID", example = "1")
    @TableField(ColumnConst.TENANT_ID)
    private Long tenantId;

}