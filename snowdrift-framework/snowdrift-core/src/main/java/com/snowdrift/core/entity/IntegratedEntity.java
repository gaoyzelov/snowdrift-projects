package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 完整实体类
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/6 16:48
 */
@Getter
@Setter
@Accessors(chain = true)
@ApiModel(value = "IntegratedEntity", description = "IntegratedEntity")
public class IntegratedEntity extends BaseEntity {

    @ApiModelProperty(value = "归属者ID", example = "1", position = 97)
    @TableField(ColumnConst.OWNER_ID)
    private Long ownerId;

    @ApiModelProperty(value = "是否删除", example = "0", position = 98)
    @TableField(ColumnConst.DELETED)
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "乐观锁", example = "1", position = 99)
    @Version
    @TableField(ColumnConst.VERSION)
    private Long version;

    @ApiModelProperty(value = "租户ID", example = "1", position = 100)
    @TableField(ColumnConst.TENANT_ID)
    private Long tenantId;
}