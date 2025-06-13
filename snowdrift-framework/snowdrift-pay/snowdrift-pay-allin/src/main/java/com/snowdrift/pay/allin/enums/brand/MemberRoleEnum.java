package com.snowdrift.pay.allin.enums.brand;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * MemberRoleEnum
 *
 * @author gaoye
 * @date 2025/06/03 16:32:34
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum MemberRoleEnum {

    STORE("00", "门店"),
    DEALER("02", "经销商"),
    PARTNERS("03", "联营商"),
    AGENT("04", "代理商"),
    OTHER("99", "其他");

    private static final Map<String, MemberRoleEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(MemberRoleEnum.values().length);
        for (MemberRoleEnum value : MemberRoleEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static MemberRoleEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}