package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.security.exception.SecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityBackendExclusivityTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description spring 与 sa-token 两安全后端互斥守卫：双开时启动期立即失败，单独启用/模块缺失时不受影响
 * @since 1.0.0
 */
class SecurityBackendExclusivityTest {

    @Test
    @DisplayName("两个后端同时启用：启动期抛 SecurityException，并在消息中指明两个开关")
    void failsFastWhenBothBackendsEnabled() {
        assertThatThrownBy(() -> SnowdriftSecuritySpringConfiguration
                .checkBackendExclusivity(true, true))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("snowdrift.security.spring.enabled")
                .hasMessageContaining("snowdrift.security.sa-token.enabled");
    }

    @Test
    @DisplayName("sa-token 模块存在但未启用：不失败（spring 后端单独工作是正常配置）")
    void okWhenSaTokenModulePresentButDisabled() {
        assertThatCode(() -> SnowdriftSecuritySpringConfiguration
                .checkBackendExclusivity(true, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sa-token 模块不在类路径上：即使属性为 true 也不失败（属性无实现承载，无从冲突）")
    void okWhenSaTokenModuleAbsent() {
        assertThatCode(() -> SnowdriftSecuritySpringConfiguration
                .checkBackendExclusivity(false, true))
                .doesNotThrowAnyException();
        assertThatCode(() -> SnowdriftSecuritySpringConfiguration
                .checkBackendExclusivity(false, false))
                .doesNotThrowAnyException();
    }
}
