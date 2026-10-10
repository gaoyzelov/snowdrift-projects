package com.snowdrift.framework.mq.rocketmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * MqRocketContainerContextBinderTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RocketMQ 容器绑定器黑盒测试：为目标容器织入代理、已是代理则原样返回、非目标 Bean 不处理
 * @since 1.0.0
 */
class MqRocketContainerContextBinderTest {

    private final MqRocketContainerContextBinder binder =
            new MqRocketContainerContextBinder(mock(MqContextPropagator.class));

    @Test
    @DisplayName("目标监听容器被织入为 AOP 代理（拦截 handleMessage）")
    void weavesProxyForTargetContainer() {
        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();

        Object result = binder.postProcessAfterInitialization(container, "container");

        assertThat(result).isNotSameAs(container);
        assertThat(AopUtils.isAopProxy(result)).isTrue();
    }

    @Test
    @DisplayName("已是 AOP 代理的容器原样返回，避免重复织入")
    void skipsAlreadyProxied() {
        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();
        Object proxied = binder.postProcessAfterInitialization(container, "container");

        Object again = binder.postProcessAfterInitialization(proxied, "container");

        assertThat(again).isSameAs(proxied);
    }

    @Test
    @DisplayName("非监听容器 Bean 原样返回，不做处理")
    void ignoresNonTargetBean() {
        Object other = new Object();

        assertThat(binder.postProcessAfterInitialization(other, "other")).isSameAs(other);
    }
}
