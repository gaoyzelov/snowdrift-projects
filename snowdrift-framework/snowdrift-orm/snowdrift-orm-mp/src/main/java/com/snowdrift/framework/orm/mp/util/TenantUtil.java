package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;

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
     *
     * @param runnable 运行任务
     */
    public static void ignore(Runnable runnable) {
        try {
            // 设置忽略租户插件
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            runnable.run();
        } finally {
            // 关闭忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /**
     * 执行忽略租户
     *
     * @param supplier 供应者
     * @param <T>  返回类型
     * @return T
     */
    public static <T> T ignore(Supplier<T> supplier) {
        try {
            // 设置忽略租户插件
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            // 执行方法
            return supplier.get();
        } finally {
            // 关闭忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }
}
