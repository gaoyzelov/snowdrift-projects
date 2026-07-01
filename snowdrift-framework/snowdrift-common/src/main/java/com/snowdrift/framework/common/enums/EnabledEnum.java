package com.snowdrift.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * EnabledEnum
 *
 * @author gaoyzelov
 * @date 2026/4/29-14:21
 * @description 启用禁用枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EnabledEnum implements IEnum<Integer> {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String note;
}
