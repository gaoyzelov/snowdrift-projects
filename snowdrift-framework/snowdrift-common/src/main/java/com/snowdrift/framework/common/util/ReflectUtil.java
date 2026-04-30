package com.snowdrift.framework.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

/**
 * ReflectUtil
 *
 * @author 83674
 * @date 2026/3/27-14:43
 * @description 反射工具类
 * @since 1.0.0
 */
@Slf4j
public final class ReflectUtil {

    private ReflectUtil() {
    }

    /**
     * 获取类全部属性
     *
     * @param t 目标类对象
     * @return 属性列表
     */
    public static <T> List<Field> getDeclaredFields(T t) {
        return getDeclaredFields(t, false);
    }

    /**
     * 获取类全部属性
     *
     * @param t                 目标类对象
     * @param includeSuperclass 是否包含父类属性
     * @return 属性列表
     */
    public static <T> List<Field> getDeclaredFields(T t, boolean includeSuperclass) {
        if (Objects.isNull(t)) {
            return Collections.emptyList();
        }
        List<Field> fields = new ArrayList<>();
        try {
            Class<?> cls = t.getClass();
            //遍历当前类及其父类属性
            while (Objects.nonNull(cls)) {
                Field[] declaredFields = cls.getDeclaredFields();
                fields.addAll(Arrays.asList(declaredFields));
                if (!includeSuperclass) {
                    break;
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception e) {
            log.error("获取属性列表失败：{}", t.getClass().getCanonicalName(), e);
        }
        return fields;
    }
}
