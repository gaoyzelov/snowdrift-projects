package com.snowdrift.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ResultEnum
 *
 * @author gaoye
 * @date 2025/03/25 09:54:30
 * @description 结果状态枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultEnum implements IEnum<Integer> {

    ERR(0, "失败"),
    OK(1, "成功");

    private final Integer code;

    private final String note;
}