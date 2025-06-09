package com.snowdrift.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.snowdrift.core.constant.ColumnConst;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "VersionEntity")
public class VersionEntity extends BaseEntity {

    @Schema(description = "乐观锁", example = "1")
    @Version
    @TableField(ColumnConst.VERSION)
    private Long version;
}