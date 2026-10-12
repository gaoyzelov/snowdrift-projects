package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;

import java.util.function.Supplier;

/**
 * TenantUtil
 *
 * @author gaoyzelov
 * @date 2026/7/1-17:21
 * @description 租户工具类
 * @since 1.0.0
 */
public final class TenantUtil {

    private TenantUtil() {
    }

    /**
     * 执行忽略租户
     * <p>
     * 可与 {@link DataScopeUtil#ignore} 嵌套：进入时按「并集」设置忽略标记，退出时恢复外层标记，
     * 因此嵌套调用在体内与退出后都不会丢掉外层的忽略。实现与理由见 {@link InterceptorIgnoreScope}。
     * </p>
     *
     * @param runnable 运行任务
     */
    public static void ignore(Runnable runnable) {
        InterceptorIgnoreScope.run(tenantIgnoreStrategy(), runnable);
    }

    /**
     * 执行忽略租户
     *
     * @param supplier 供应者
     * @param <T>      返回类型
     * @return T
     */
    public static <T> T ignore(Supplier<T> supplier) {
        return InterceptorIgnoreScope.call(tenantIgnoreStrategy(), supplier);
    }

    private static IgnoreStrategy tenantIgnoreStrategy() {
        return IgnoreStrategy.builder().tenantLine(true).build();
    }
}
