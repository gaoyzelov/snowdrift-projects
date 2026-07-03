package com.snowdrift.framework.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author 83674
 * @date 2026/7/2-14:09
 * @description
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum implements IEnum<Integer> {

    ALL(0, "全部"),
    DEPT(1, "本部门"),
    DEPT_AND_SUB(2, "本部门及子部门"),
    SELF(3, "仅自己"),
    CUSTOM(4, "自定义");

    @JsonValue
    private final Integer code;

    private final String note;

    @JsonCreator
    public static DataScopeEnum of(Integer code) {
        // 默认仅查看自己数据
        return IEnum.getByCode(DataScopeEnum.class, code).orElse(SELF);
    }
}
