package com.snowdrift.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * EnabledEnum
 *
 * @author 83674
 * @date 2026/4/29-14:21
 * @description 启用禁用枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EnabledEnum implements IEnum<Integer> {

    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String note;
}
