package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * IdEntity
 *
 * @author gaoyzelov
 * @date 2026/7/1-16:00
 * @description ID基类（通用字段）
 * @since 1.0.0
 */
@Data
public class IdEntity implements Serializable {

    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
}
