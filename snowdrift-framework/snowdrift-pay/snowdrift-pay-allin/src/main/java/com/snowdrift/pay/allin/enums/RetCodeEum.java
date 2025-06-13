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
public enum RetCodeEum {

    SUCCESS("SUCCESS", "成功"),

    FAIL("FAIL", "失败");

    private final String code;

    private final String note;
}