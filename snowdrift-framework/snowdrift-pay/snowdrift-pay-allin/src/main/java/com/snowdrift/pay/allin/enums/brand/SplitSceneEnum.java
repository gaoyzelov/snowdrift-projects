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
public enum SplitSceneEnum {

    BRAND("00", "品牌方分账"),
    AGENT("01", "代理商分账"),
    PARTNERS("02", "联营商分账"),
    DEALER("03", "经销商分账"),
    STORE("04", "门店分账");

    private static final Map<String, SplitSceneEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(SplitSceneEnum.values().length);
        for (SplitSceneEnum value : SplitSceneEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static SplitSceneEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}