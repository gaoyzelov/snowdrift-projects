package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 租户忽略包装器黑盒测试：Supplier 返回值、Runnable 执行、异常传播且 finally 清理不掩盖原异常
 * @since 1.0.0
 */
class TenantUtilTest {

    @Test
    @DisplayName("ignore(Supplier) 返回回调结果")
    void supplierReturnsValue() {
        String result = TenantUtil.ignore(() -> "value");

        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("ignore(Runnable) 执行回调")
    void runnableExecutes() {
        AtomicBoolean ran = new AtomicBoolean(false);

        TenantUtil.ignore(() -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("回调抛异常时原样上抛，finally 清理不掩盖异常")
    void propagatesException() {
        // 显式类型化，避免“总是抛异常”的 lambda 在 Runnable/Supplier 重载间产生歧义
        Runnable failingRunnable = () -> {
            throw new IllegalStateException("boom");
        };
        java.util.function.Supplier<Object> failingSupplier = () -> {
            throw new IllegalStateException("boom2");
        };

        assertThatThrownBy(() -> TenantUtil.ignore(failingRunnable))
                .isInstanceOf(IllegalStateException.class).hasMessage("boom");
        assertThatThrownBy(() -> TenantUtil.ignore(failingSupplier))
                .isInstanceOf(IllegalStateException.class).hasMessage("boom2");
    }

    @Test
    @DisplayName("回调执行后忽略策略被清理：再次调用仍正常工作")
    void clearAllowsReuse() {
        assertThat(TenantUtil.ignore(() -> 1)).isEqualTo(1);
        assertThat(TenantUtil.ignore(() -> 2)).isEqualTo(2);
    }

    @Test
    @DisplayName("嵌套：内层忽略数据权限时，体内与退出后都必须保留外层的租户忽略")
    void nestedIgnoreKeepsOuterTenantFlag() {
        TenantUtil.ignore(() -> {
            assertThat(tenantIgnored()).isTrue();

            DataScopeUtil.ignore(() -> {
                // 体内：内层只是「追加」数据权限忽略，不能把外层的租户忽略顶掉
                assertThat(tenantIgnored()).isTrue();
                assertThat(dataPermissionIgnored()).isTrue();
            });

            // 内层退出后：外层仍在生效，且内层的数据权限忽略已回收
            assertThat(tenantIgnored()).isTrue();
            assertThat(dataPermissionIgnored()).isFalse();
        });

        assertThat(InterceptorIgnoreHelper.hasIgnoreStrategy()).isFalse();
    }

    @Test
    @DisplayName("嵌套：内层抛异常被外层捕获后，外层忽略必须恢复")
    void nestedExceptionKeepsOuterTenantFlag() {
        TenantUtil.ignore(() -> {
            try {
                DataScopeUtil.ignore(() -> {
                    throw new IllegalStateException("boom");
                });
            } catch (IllegalStateException ignored) {
                assertThat(tenantIgnored()).isTrue();
            }
        });

        assertThat(InterceptorIgnoreHelper.hasIgnoreStrategy()).isFalse();
    }

    /** 用 MP 公开 API 探测当前线程的租户忽略标记（探针 id 不会命中注解缓存） */
    private static boolean tenantIgnored() {
        return InterceptorIgnoreHelper.willIgnoreTenantLine("snowdrift-probe");
    }

    /** 用 MP 公开 API 探测当前线程的数据权限忽略标记 */
    private static boolean dataPermissionIgnored() {
        return InterceptorIgnoreHelper.willIgnoreDataPermission("snowdrift-probe");
    }
}
