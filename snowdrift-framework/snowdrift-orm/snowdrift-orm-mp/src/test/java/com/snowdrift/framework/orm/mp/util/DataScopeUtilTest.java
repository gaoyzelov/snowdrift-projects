package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataScopeUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 数据权限忽略包装器黑盒测试：Supplier 返回值、Runnable 执行、异常传播且 finally 清理不掩盖原异常
 * @since 1.0.0
 */
class DataScopeUtilTest {

    @Test
    @DisplayName("ignore(Supplier) 返回回调结果")
    void supplierReturnsValue() {
        assertThat(DataScopeUtil.ignore(() -> "v")).isEqualTo("v");
    }

    @Test
    @DisplayName("ignore(Runnable) 执行回调")
    void runnableExecutes() {
        AtomicBoolean ran = new AtomicBoolean(false);

        DataScopeUtil.ignore(() -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("回调抛异常时原样上抛，finally 清理不掩盖异常")
    void propagatesException() {
        Runnable failingRunnable = () -> {
            throw new IllegalStateException("ds-boom");
        };
        Supplier<Object> failingSupplier = () -> {
            throw new IllegalStateException("ds-boom2");
        };

        assertThatThrownBy(() -> DataScopeUtil.ignore(failingRunnable))
                .isInstanceOf(IllegalStateException.class).hasMessage("ds-boom");
        assertThatThrownBy(() -> DataScopeUtil.ignore(failingSupplier))
                .isInstanceOf(IllegalStateException.class).hasMessage("ds-boom2");
    }

    @Test
    @DisplayName("嵌套（反方向）：外层忽略数据权限、内层忽略租户时，两者在体内共存且退出后正确回收")
    void nestedIgnoreKeepsOuterDataPermissionFlag() {
        DataScopeUtil.ignore(() -> {
            assertThat(dataPermissionIgnored()).isTrue();

            TenantUtil.ignore(() -> {
                assertThat(dataPermissionIgnored()).isTrue();
                assertThat(tenantIgnored()).isTrue();
            });

            assertThat(dataPermissionIgnored()).isTrue();
            assertThat(tenantIgnored()).isFalse();
        });

        assertThat(InterceptorIgnoreHelper.hasIgnoreStrategy()).isFalse();
    }

    private static boolean tenantIgnored() {
        return InterceptorIgnoreHelper.willIgnoreTenantLine("snowdrift-probe");
    }

    private static boolean dataPermissionIgnored() {
        return InterceptorIgnoreHelper.willIgnoreDataPermission("snowdrift-probe");
    }
}
