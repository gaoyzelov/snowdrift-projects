package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "TenantEntity", description = "TenantEntity")
public class TenantEntity extends BaseEntity {

    @ApiModelProperty(value = "租户ID", example = "1", position = 97)
    @TableField(ColumnConst.TENANT_ID)
    private Long tenantId;

}