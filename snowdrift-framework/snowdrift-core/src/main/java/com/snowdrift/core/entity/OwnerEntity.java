package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.annotations.ApiModelProperty;
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
public class OwnerEntity extends BaseEntity {

    @ApiModelProperty(value = "归属者ID", example = "1", position = 97)
    @TableField(ColumnConst.OWNER_ID)
    private Long ownerId;
}