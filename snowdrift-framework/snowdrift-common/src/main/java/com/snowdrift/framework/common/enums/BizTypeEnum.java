package com.snowdrift.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BizTypeEnum
 * @author 83674
 * @date 2026/4/29-15:12
 * @description 业务类型枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum implements IEnum<Integer> {

    INSERT(0, "新增"),
    UPDATE(1, "修改"),
    DELETE(2, "删除"),
    SELECT(3, "查询"),
    EXPORT(4, "导出"),
    IMPORT(5, "导入"),
    OTHER(9, "其他");

    private final Integer code;
    private final String note;
}
