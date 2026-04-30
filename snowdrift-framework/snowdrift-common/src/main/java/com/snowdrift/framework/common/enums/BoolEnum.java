package com.snowdrift.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BoolEnum
 * @author 83674
 * @date 2026/4/29-14:20
 * @description 布尔枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BoolEnum implements IEnum {

    FALSE(0, "否"),
    TRUE(1, "是");

    private final Integer code;

    private final String note;
}
