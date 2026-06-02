package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * AssertUtil
 *
 * @author 83674
 * @date 2026/3/27-13:19
 * @description 断言工具类
 * @since 1.0.0
 */
public final class AssertUtil {
    private AssertUtil() {
    }

    /**
     * 断言对象不为空
     *
     * @param o       对象
     * @param message 错误信息
     */
    public static void notNull(Object o, String message) {
        if (Objects.isNull(o)) {
            throw new BizException(message);
        }
    }

    /**
     * 断言字符串非空
     *
     * @param text    待断言字符串
     * @param message 错误信息
     */
    public static void notBlank(String text, String message) {
        if (StringUtils.isBlank(text)) {
            throw new BizException(message);
        }
    }


    /**
     * 断言集合不为空
     *
     * @param collection 集合
     * @param message    错误信息
     */
    public static <T> void notEmpty(Collection<T> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BizException(message);
        }
    }

    /**
     * 断言数组不为空
     *
     * @param array   数组
     * @param message 错误信息
     */
    public static <T> void notEmpty(T[] array, String message) {
        if (ArrayUtils.isEmpty(array)) {
            throw new BizException(message);
        }
    }

    /**
     * 断言集合中包含指定元素
     *
     * @param item       待断言元素
     * @param collection 集合
     * @param message    错误信息
     */
    public static <T> void inside(T item, Collection<T> collection, String message) {
        AssertUtil.notNull(item, "元素不能为空");
        AssertUtil.notEmpty(collection, "集合不能为空");
        if (!collection.contains(item)) {
            throw new BizException(message);
        }
    }

    /**
     * 断言条件为真
     *
     * @param predicate 断言条件
     * @param message   错误信息
     */
    public static void isTrue(boolean predicate, String message) {
        if (!predicate) {
            throw new BizException(message);
        }
    }

    /**
     * 断言条件为真
     *
     * @param predicate 断言条件
     * @param message   错误信息
     */
    public static void custom(Supplier<Boolean> predicate, String message) {
        if (!predicate.get()){
            throw new BizException(message);
        }
    }
}
