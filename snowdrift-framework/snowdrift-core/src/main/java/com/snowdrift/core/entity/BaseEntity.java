package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基本实体
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/6 16:16
 */
@Getter
@Setter
@Accessors(chain = true)
@Schema(description = "BaseEntity")
public class BaseEntity implements Serializable {

    @Schema(description = "创建人", example = "张三")
    @TableField(ColumnConst.CREATE_BY)
    private String createBy;

    @Schema(description = "创建时间", example = "2024-08-06 16:16:00")
    @TableField(value = ColumnConst.CREATE_TIME, fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "李四")
    @TableField(ColumnConst.UPDATE_BY)
    private String updateBy;

    @Schema(description = "更新时间", example = "2024-08-06 16:16:00")
    @TableField(value = ColumnConst.UPDATE_TIME, fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}