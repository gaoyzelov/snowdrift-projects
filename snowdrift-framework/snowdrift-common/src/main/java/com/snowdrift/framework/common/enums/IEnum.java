package com.snowdrift.framework.common.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * IEnum
 * 
 * @author 83674
 * @date 2026/3/25-15:57
 * @description 通用枚举接口
 * @since 1.0.0
 */
public interface IEnum<E> {

    /**
     * 获取枚举值
     * 
     * @return code
     */
    E getCode();

    /**
     * 获取枚举描述
     * 
     * @return note
     */
    String getNote();

    /**
     * 根据枚举值获取枚举对象
     *
     * @param enumClass 枚举类
     * @param code      枚举值
     * @return 枚举对象
     */
    static <T extends IEnum<E>,E> Optional<T> getByCode(Class<T> enumClass, E code) {
        T[] enumConstants = enumClass.getEnumConstants();
        for (T enumConstant : enumConstants) {
            if (Objects.equals(enumConstant.getCode(), code)) {
                return Optional.of(enumConstant);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据枚举描述获取枚举对象
     *
     * @param enumClass 枚举类
     * @param note      枚举描述
     * @return 枚举对象
     */
    static <T extends IEnum<E>,E> Optional<T> getByNote(Class<T> enumClass, String note) {
        T[] enumConstants = enumClass.getEnumConstants();
        for (T enumConstant : enumConstants) {
            if (StringUtils.equals(enumConstant.getNote(), note)) {
                return Optional.of(enumConstant);
            }
        }
        return Optional.empty();
    }
}
