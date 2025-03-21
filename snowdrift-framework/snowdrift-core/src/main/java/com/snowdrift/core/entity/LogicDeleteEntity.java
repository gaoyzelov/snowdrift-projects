package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * LogicDeleteEntity
 *
 * @author gaoye
 * @date 2024/12/26 19:41:42
 * @description 逻辑删除
 * @since 1.0
 */
@Getter
@Setter
@ApiModel(value = "LogicDeleteEntity", description = "LogicDeleteEntity")
public class LogicDeleteEntity extends BaseEntity {

    @ApiModelProperty(value = "是否删除，0-未删除，1-已删除", example = "0", position = 97)
    @TableField(ColumnConst.DELETED)
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}