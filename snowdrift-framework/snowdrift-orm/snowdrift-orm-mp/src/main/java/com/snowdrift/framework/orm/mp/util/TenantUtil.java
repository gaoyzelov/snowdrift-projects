package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author 83674
 * @date 2026/7/1-17:21
 * @description
 * @since 1.0.0
 */
public final class TenantUtil {

    private TenantUtil() {
    }

    public static <T> void ignore(Consumer<T> consumer) {
        try {
            // 设置忽略租户插件
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            consumer.accept(null);
        } finally {
            // 关闭忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /**
     * 执行忽略租户
     *
     * @param func 函数
     * @param <T>  返回类型
     * @return T
     */
    public static <T> T ignore(Supplier<T> func) {
        try {
            // 设置忽略租户插件
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            // 执行方法
            return func.get();
        } finally {
            // 关闭忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }
}
