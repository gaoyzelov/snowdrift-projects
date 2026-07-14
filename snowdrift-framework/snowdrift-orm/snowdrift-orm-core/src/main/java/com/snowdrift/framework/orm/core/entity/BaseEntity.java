package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
public class BaseEntity implements Serializable {

    /**
     * 主键ID（数据库自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
}
