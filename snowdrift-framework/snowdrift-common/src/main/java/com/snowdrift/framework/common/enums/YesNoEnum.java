package com.snowdrift.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * YesNoEnum
 * @author gaoyzelov
 * @date 2026/4/29-14:20
 * @description 布尔枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum YesNoEnum implements IEnum<Integer> {

    NO(0, "否"),
    YES(1, "是");

    private final Integer code;

    private final String note;
}
