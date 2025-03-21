package com.snowdrift.core.utils;

import com.snowdrift.core.exception.BaseException;

import java.lang.reflect.Field;
import java.util.*;

/**
 * ReflectUtils
 *
 * @author gaoye
 * @date 2025/03/21 10:09:26
 * @description 反射工具类
 * @since 1.0.0
 */
public class ReflectUtils {

    /**
     * 获取当前类及其父类的所有属性
     *
     * @param clazz 当前类
     * @return 属性列表
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        return getAllFields(clazz, true);
    }

    /**
     * 获取当前类及其父类的所有属性
     *
     * @param clazz             当前类
     * @param includeSuperClass 是否包含父类属性
     * @return 属性列表
     */
    public static List<Field> getAllFields(Class<?> clazz, boolean includeSuperClass) {
        List<Field> fields = new ArrayList<>();
        try {
            while (Objects.nonNull(clazz)) {
                //遍历当前类属性
                Field[] declaredFields = clazz.getDeclaredFields();
                fields.addAll(Arrays.asList(declaredFields));
                if (includeSuperClass) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new BaseException("获取属性信息异常", e);
        }
        return fields;
    }
}