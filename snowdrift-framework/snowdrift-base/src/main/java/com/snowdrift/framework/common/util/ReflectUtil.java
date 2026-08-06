package com.snowdrift.framework.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ReflectUtil
 *
 * @author gaoyzelov
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
     * 根据方法名查找 Method 对象
     * <p>
     * 仅查找当前类的声明方法（不含父类），返回前调用 {@code setAccessible(true)}。
     * 若存在重载方法，仅返回第一个匹配（JVM 不保证 {@code getDeclaredMethods()} 顺序），
     * 同时输出 WARN 日志提醒调用方应使用含参数类型的重载方法。
     * </p>
     *
     * @param clazz 目标类
     * @param name  方法名
     * @return Method 对象
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    public static Method getMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        Method result = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                if (result == null) {
                    result = m;
                } else {
                    log.warn("类 {} 存在重载方法 {}，getMethod 仅返回第一个匹配，建议使用带参数类型的重载方法",
                            clazz.getName(), name);
                    break;
                }
            }
        }
        if (result != null) {
            result.setAccessible(true);
            return result;
        }
        throw new NoSuchMethodException(clazz.getName() + "#" + name);
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
