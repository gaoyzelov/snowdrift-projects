package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RetCodeEum
 *
 * @author gaoye
 * @date 2025/05/22 11:05:39
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ValidateTypeEum {

    NONE(0L, "无验证"),

    SMS(1L, "短信验证码");

    private final Long code;

    private final String note;
}