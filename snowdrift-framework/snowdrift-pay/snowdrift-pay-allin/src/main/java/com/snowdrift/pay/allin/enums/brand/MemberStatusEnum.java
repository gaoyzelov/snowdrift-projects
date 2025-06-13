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
public enum MemberStatusEnum {

    STORE(1L, "待审核"),
    DEALER(2L, "审核成功"),
    PARTNERS(2L, "审核失败");

    private static final Map<Long, MemberStatusEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(MemberStatusEnum.values().length);
        for (MemberStatusEnum value : MemberStatusEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final Long code;

    private final String note;

    public static MemberStatusEnum getByCode(Long code) {
        return CODE_MAP.get(code);
    }
}