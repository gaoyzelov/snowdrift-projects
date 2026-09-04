package com.snowdrift.framework.base.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DataScopeEnum
 *
 * @author gaoyzelov
 * @date 2026/7/2-14:09
 * @description 数据权限范围枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum implements IEnum<Integer> {

    NONE(-1, "无权限"),
    ALL(0, "全部"),
    DEPT(1, "本部门"),
    DEPT_AND_SUB(2, "本部门及子部门"),
    SELF(3, "仅自己"),
    CUSTOM(4, "自定义");

    @JsonValue
    private final Integer code;

    private final String note;

    /**
     * 将存储的数值解析为数据权限范围枚举（用于 JSON 反序列化及业务转换）。
     * <p>入参为 {@code null} 或无法识别的编码时，统一回退到 {@link #NONE}（无权限），
     * 避免因配置缺失或脏数据意外扩大数据可见范围。</p>
     *
     * @param code 数据权限范围编码
     * @return 对应枚举；缺省/未知时返回 {@link #NONE}
     */
    @JsonCreator
    public static DataScopeEnum of(Integer code) {
        if (code == null) {
            return NONE;
        }
        return IEnum.getByCode(DataScopeEnum.class, code).orElse(NONE);
    }
}
