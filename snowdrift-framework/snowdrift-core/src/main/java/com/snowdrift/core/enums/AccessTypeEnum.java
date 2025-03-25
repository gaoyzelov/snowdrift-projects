package com.snowdrift.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AccessTypeEnum
 *
 * @author gaoye
 * @date 2025/03/25 09:48:48
 * @description 接口访问类型枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AccessTypeEnum implements IEnum<Integer> {

    GENERAL(0, "普通"),
    LOGIN(1, "登录"),
    LOGOUT(2, "登出");

    private final Integer code;

    private final String note;
}