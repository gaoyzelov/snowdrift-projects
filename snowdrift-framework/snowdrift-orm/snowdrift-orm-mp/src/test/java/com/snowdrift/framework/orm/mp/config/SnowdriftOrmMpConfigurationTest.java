package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.orm.mp.CryptoKeyHolder;
import com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler;
import com.snowdrift.framework.orm.mp.properties.OrmMpBaseProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpPaginationProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowdriftOrmMpConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description ORM 自动配置黑盒测试：AES 密钥启动校验（长度/十六进制/fail-fast）、拦截器装配与顺序、自动填充处理器
 * @since 1.0.0
 */
class SnowdriftOrmMpConfigurationTest {

    @AfterEach
    void reset() {
        CryptoKeyHolder.setKey(null);
    }

    private SnowdriftOrmMpConfiguration config(Boolean crypto, String key) {
        OrmMpBaseProperties props = new OrmMpBaseProperties();
        props.setCrypto(crypto);
        props.setCryptoKey(key);
        return new SnowdriftOrmMpConfiguration(props);
    }

    // ========== 密钥启动校验 ==========

    @Test
    @DisplayName("crypto 关闭时不校验密钥，缺密钥也不抛错")
    void cryptoDisabledSkipsValidation() {
        assertThatCode(() -> config(false, null).cryptoKeyInitializer()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("crypto 开启但未配置密钥时启动失败")
    void cryptoEnabledBlankKeyFails() {
        assertThatThrownBy(() -> config(true, "  ").cryptoKeyInitializer())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密钥未配置");
    }

    @Test
    @DisplayName("密钥长度为奇数时启动失败")
    void oddLengthKeyFails() {
        assertThatThrownBy(() -> config(true, "abc").cryptoKeyInitializer())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("偶数位");
    }

    @Test
    @DisplayName("密钥字节数非 16/24/32 时启动失败")
    void wrongByteLengthKeyFails() {
        // 20 位十六进制 = 10 字节，非法
        assertThatThrownBy(() -> config(true, "00112233445566778899").cryptoKeyInitializer())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("长度无效");
    }

    @Test
    @DisplayName("密钥长度合法但含非十六进制字符时启动失败（fail-fast，本次修复点）")
    void nonHexKeyFailsAtStartup() {
        // 32 位（=16 字节）但非十六进制字符
        String nonHex = "z".repeat(32);

        assertThatThrownBy(() -> config(true, nonHex).cryptoKeyInitializer())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("十六进制");
    }

    @Test
    @DisplayName("合法十六进制密钥（AES-128/192/256）通过校验并初始化到持有器")
    void validKeyInitializesHolder() {
        config(true, "0123456789abcdef0123456789abcdef").cryptoKeyInitializer();
        assertThat(CryptoKeyHolder.getKey()).isEqualTo("0123456789abcdef0123456789abcdef");

        config(true, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").cryptoKeyInitializer();
        assertThat(CryptoKeyHolder.isKeyAvailable()).isTrue();
    }

    // ========== 拦截器装配 ==========

    private MybatisPlusInterceptor interceptor(boolean tenantEnabled, boolean optimisticLock) {
        OrmMpBaseProperties base = new OrmMpBaseProperties();
        base.setOptimisticLock(optimisticLock);
        SnowdriftOrmMpConfiguration configuration = new SnowdriftOrmMpConfiguration(base);
        OrmMpTenantProperties tenantProps = new OrmMpTenantProperties();
        tenantProps.setEnabled(tenantEnabled);
        return configuration.mybatisPlusInterceptor(new OrmMpPaginationProperties(), tenantProps,
                Optional.<IDataScopeProvider>empty());
    }

    @Test
    @DisplayName("租户关闭、乐观锁关闭：装配数据权限+防全表+分页，分页置于末位")
    void interceptorsWithoutTenantAndOptimisticLock() {
        MybatisPlusInterceptor interceptor = interceptor(false, false);
        List<InnerInterceptor> inners = interceptor.getInterceptors();

        assertThat(inners).hasSize(3);
        assertThat(inners).noneMatch(i -> i instanceof TenantLineInnerInterceptor);
        assertThat(inners).noneMatch(i -> i instanceof OptimisticLockerInnerInterceptor);
        assertThat(inners.get(inners.size() - 1)).isInstanceOf(PaginationInnerInterceptor.class);
    }

    @Test
    @DisplayName("租户开启：额外装配 TenantLineInnerInterceptor")
    void interceptorsWithTenant() {
        List<InnerInterceptor> inners = interceptor(true, false).getInterceptors();

        assertThat(inners).hasSize(4);
        assertThat(inners).anyMatch(i -> i instanceof TenantLineInnerInterceptor);
    }

    @Test
    @DisplayName("乐观锁开启：额外装配 OptimisticLockerInnerInterceptor")
    void interceptorsWithOptimisticLock() {
        List<InnerInterceptor> inners = interceptor(false, true).getInterceptors();

        assertThat(inners).anyMatch(i -> i instanceof OptimisticLockerInnerInterceptor);
    }

    @Test
    @DisplayName("装配字段自动填充处理器")
    void registersFieldAutoFillHandler() {
        SnowdriftOrmMpConfiguration configuration = new SnowdriftOrmMpConfiguration(new OrmMpBaseProperties());

        MetaObjectHandler handler = configuration.fieldAutoFillHandler(new OrmMpTenantProperties());

        assertThat(handler).isInstanceOf(FieldAutoFillHandler.class);
    }
}
