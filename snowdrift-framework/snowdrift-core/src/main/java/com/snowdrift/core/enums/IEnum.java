package com.snowdrift.core.enums;

/**
 * IEnum
 *
 * @author gaoye
 * @date 2025/03/18 16:18:57
 * @description 枚举接口
 * @since 1.0.0
 */
public interface IEnum<T> {

    Integer getCode();

    String getNote();

    default T getByCode(Integer code){
        return null;
    }
}