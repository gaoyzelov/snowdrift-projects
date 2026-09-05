package com.snowdrift.framework.base.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.snowdrift.framework.base.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * StatusEnum
 *
 * @author gaoyzelov
 * @date 2026/4/29-14:21
 * @description 数据状态枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum StatusEnum implements IEnum<Integer> {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @JsonValue
    private final Integer code;

    private final String note;

    /**
     * 反序列化：按编码解析（与 {@link #getCode()} 输出一致）。
     * <p>无法识别的编码抛 {@link BizException}，由全局异常处理兜底为参数错误。</p>
     *
     * @param code 枚举编码
     * @return 对应枚举
     */
    @JsonCreator
    public static StatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        return IEnum.getByCode(StatusEnum.class, code)
                .orElseThrow(() -> new BizException("状态有误: " + code));
    }
}
