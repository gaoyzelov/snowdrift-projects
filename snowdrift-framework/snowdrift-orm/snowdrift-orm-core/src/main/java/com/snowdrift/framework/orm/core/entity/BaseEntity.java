package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类（通用字段）
 * <p>
 * 所有数据库实体继承此类即可自动获得以下能力：
 * <ul>
 *   <li><b>自增主键</b>：{@link #id}，数据库自增策略</li>
 *   <li><b>自动填充</b>：{@link #createBy}、{@link #createTime} 由 {@link com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler} 在 INSERT 时自动写入</li>
 *   <li><b>自动更新</b>：{@link #updateBy}、{@link #updateTime} 在 INSERT 和 UPDATE 时自动写入</li>
 *   <li><b>逻辑删除</b>：{@link #deleted}，配合 MyBatis-Plus {@link TableLogic} 实现软删除</li>
 * </ul>
 * </p>
 *
 * @author 83674
 * @date 2026/7/1-16:00
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
    @TableLogic
    private Integer deleted;
}
