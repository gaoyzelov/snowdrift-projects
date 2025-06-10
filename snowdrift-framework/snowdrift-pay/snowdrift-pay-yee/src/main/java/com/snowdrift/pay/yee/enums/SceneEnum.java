package com.snowdrift.pay.yee.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * CodeEnum
 *
 * @author gaoye
 * @date 2025/06/06 09:21:25
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum SceneEnum {

    ONLINE("ONLINE", "线上"),
    OFFLINE("OFFLINE", "线下"),
    BAOXIAN("BAOXIAN", "保险"),
    GONGYI("GONGYI", "公益"),
    DC_SEPARATION("DC_SEPARATION", "借贷分离"),
    DIGITAL("DIGITAL", "数娱"),
    REGISTRATION("REGISTRATION", "报名"),
    PRIVATE_EDUCATION("PRIVATE_EDUCATION", "民办教育"),
    DIRECT("DIRECT", "直连"),
    LARGE("LARGE", "特殊"),
    STORE_ASST("STORE_ASST", "门店助手");

    private static final Map<String, SceneEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (SceneEnum value : values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static SceneEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}