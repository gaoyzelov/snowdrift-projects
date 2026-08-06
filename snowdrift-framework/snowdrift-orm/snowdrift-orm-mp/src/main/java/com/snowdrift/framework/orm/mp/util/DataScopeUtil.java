package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;

/**
 * 数据权限工具类 — 提供临时跳过数据权限过滤的能力
 * <p>
 * 用于特定场景（如管理员操作、内部数据同步等）需要绕过数据权限过滤时使用。
 * 用法：
 * <pre>{@code
 * // 无返回值
 * DataScopeUtil.ignore(() -> userService.getPage(pageDTO, queryDTO));
 *
 * // 有返回值
 * List<User> users = DataScopeUtil.ignore(() -> userService.getList());
 * }</pre>
 * </p>
 *
 * @author gaoye
 * @since 1.0.0
 */
public final class DataScopeUtil {

    private DataScopeUtil() {
    }

    /**
     * 在忽略数据权限的上下文中执行无返回值的操作
     *
     * @param runnable 要执行的操作
     */
    public static void ignore(Runnable runnable) {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).build());
        try {
            runnable.run();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /**
     * 在忽略数据权限的上下文中执行有返回值的操作
     *
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     */
    public static <T> T ignore(java.util.function.Supplier<T> supplier) {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).build());
        try {
            return supplier.get();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }
}
