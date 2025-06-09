package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "IntegratedEntity")
public class IntegratedEntity extends BaseEntity {

    @Schema(description = "归属者ID", example = "1")
    @TableField(ColumnConst.OWNER_ID)
    private Long ownerId;

    @Schema(description = "是否删除", example = "0")
    @TableField(ColumnConst.DELETED)
    @TableLogic
    private Integer deleted;

    @Schema(description = "乐观锁", example = "1")
    @Version
    @TableField(ColumnConst.VERSION)
    private Long version;

    @Schema(description = "租户ID", example = "1")
    @TableField(ColumnConst.TENANT_ID)
    private Long tenantId;
}