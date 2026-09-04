package com.snowdrift.framework.base.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BizTypeEnum
 * @author gaoyzelov
 * @date 2026/4/29-15:12
 * @description 业务类型枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum implements IEnum<Integer> {

    INSERT(0, "新增"),
    UPDATE(1, "修改"),
    DELETE(2, "删除"),
    SELECT(3, "查询"),
    EXPORT(4, "导出"),
    IMPORT(5, "导入"),
    OTHER(9, "其他");

    @JsonValue
    private final Integer code;

    private final String note;

    /**
     * 反序列化：按编码解析（与 {@link #getCode()} 输出一致）。
     * <p>无法识别的编码抛 {@link IllegalArgumentException}，由全局异常处理兜底为参数错误。</p>
     *
     * @param code 枚举编码
     * @return 对应枚举
     */
    @JsonCreator
    public static BizTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        return IEnum.getByCode(BizTypeEnum.class, code)
                .orElseThrow(() -> new IllegalArgumentException("无法识别的 BizTypeEnum 编码: " + code));
    }
}
