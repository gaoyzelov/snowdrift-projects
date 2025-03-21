package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 乐观锁实体类
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/6 16:45
 */
@Getter
@Setter
@ApiModel(value = "VersionEntity", description = "VersionEntity")
public class VersionEntity extends BaseEntity {

    @ApiModelProperty(value = "乐观锁", example = "1", position = 97)
    @Version
    @TableField(ColumnConst.VERSION)
    private Long version;
}