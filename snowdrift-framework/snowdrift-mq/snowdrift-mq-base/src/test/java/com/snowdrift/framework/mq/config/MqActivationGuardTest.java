package com.snowdrift.framework.mq.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MqActivationGuardTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 实现模块互斥守卫单元测试：0/1 个启用放行，多个启用启动失败
 * @since 1.0.0
 */
class MqActivationGuardTest {

    @Test
    @DisplayName("未启用任何实现时放行")
    void noneEnabledPasses() {
        MockEnvironment env = new MockEnvironment();

        assertThatCode(() -> new MqActivationGuard(env)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("仅启用一个实现时放行")
    void singleEnabledPasses() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("snowdrift.mq.kafka.enabled", "true");
        env.setProperty("snowdrift.mq.rabbitmq.enabled", "false");

        assertThatCode(() -> new MqActivationGuard(env)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("同时启用两个实现时启动失败并列出冲突开关")
    void twoEnabledFails() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("snowdrift.mq.kafka.enabled", "true");
        env.setProperty("snowdrift.mq.rocketmq.enabled", "true");

        assertThatThrownBy(() -> new MqActivationGuard(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只允许启用一种实现")
                .hasMessageContaining("snowdrift.mq.kafka.enabled")
                .hasMessageContaining("snowdrift.mq.rocketmq.enabled");
    }

    @Test
    @DisplayName("三个实现全部启用时启动失败")
    void allEnabledFails() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("snowdrift.mq.kafka.enabled", "true");
        env.setProperty("snowdrift.mq.rabbitmq.enabled", "true");
        env.setProperty("snowdrift.mq.rocketmq.enabled", "true");

        assertThatThrownBy(() -> new MqActivationGuard(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3");
    }
}
