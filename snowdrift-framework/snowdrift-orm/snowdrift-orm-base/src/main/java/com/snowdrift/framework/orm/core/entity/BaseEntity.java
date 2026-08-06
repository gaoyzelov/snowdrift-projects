package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseEntity
 *
 * @author gaoyzelov
 * @date 2026/7/1-16:00
 * @description 实体基类（通用字段）
 * @since 1.0.0
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 创建人（INSERT 时自动填充当前操作者名称）
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间（INSERT 时自动填充当前时间）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人（INSERT 和 UPDATE 时自动填充当前操作者名称）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间（INSERT 和 UPDATE 时自动填充当前时间）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志<br>
     * 0 — 未删除（正常）<br>
     * 1 — 已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /**
     * 逻辑删除时间戳
     */
    private Long deletedTime;
}
