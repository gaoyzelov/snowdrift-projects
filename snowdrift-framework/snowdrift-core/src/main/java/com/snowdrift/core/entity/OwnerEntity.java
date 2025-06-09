package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 所有者实体类
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/6 17:36
 */
@Getter
@Setter
@Schema(description = "OwnerEntity")
public class OwnerEntity extends BaseEntity {

    @Schema(description = "归属者ID", example = "1")
    @TableField(ColumnConst.OWNER_ID)
    private Long ownerId;
}