package com.snowdrift.framework.orm.mp.util;

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
}
