package com.snowdrift.framework.mq.rocketmq.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MqRocketBinderFactoryMethodTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description Binder 是 BeanPostProcessor，其 @Bean 工厂方法必须为 static：
 *              非 static 会迫使配置类提前实例化，且容器对这种装配顺序劣化不打日志
 * @since 1.0.0
 */
class MqRocketBinderFactoryMethodTest {

    @Test
    @DisplayName("mqRocketContainerContextBinder 的工厂方法必须是 static")
    void binderFactoryMethodIsStatic() {
        Method method = Arrays.stream(SnowdriftRocketMqConfiguration.class.getDeclaredMethods())
                .filter(m -> "mqRocketContainerContextBinder".equals(m.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 mqRocketContainerContextBinder 工厂方法"));

        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("BeanPostProcessor 的工厂方法必须 static，否则配置类会被提前实例化")
                .isTrue();
    }
}
