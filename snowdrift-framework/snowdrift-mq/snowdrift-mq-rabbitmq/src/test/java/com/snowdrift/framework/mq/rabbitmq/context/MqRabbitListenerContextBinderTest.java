package com.snowdrift.framework.mq.rabbitmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * MqRabbitListenerContextBinderTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RabbitMQ 容器工厂 adviceChain 绑定器黑盒测试：挂载、与既有 advice 合并、非目标 Bean 原样返回
 * @since 1.0.0
 */
class MqRabbitListenerContextBinderTest {

    private final MqRabbitContextAdvice contextAdvice = new MqRabbitContextAdvice(mock(MqContextPropagator.class));
    private final MqRabbitListenerContextBinder binder = new MqRabbitListenerContextBinder(contextAdvice);

    @Test
    @DisplayName("目标容器工厂无既有 adviceChain 时，挂载本框架 advice")
    void bindsWhenNoExistingChain() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        Object returned = binder.postProcessAfterInitialization(factory, "factory");

        assertThat(returned).isSameAs(factory);
        assertThat(factory.getAdviceChain()).containsExactly(contextAdvice);
    }

    @Test
    @DisplayName("已有 adviceChain 时合并（保留既有 + 追加本框架 advice），不整体覆盖")
    void mergesWithExistingChain() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        Advice existing = new NoOpAdvice();
        factory.setAdviceChain(existing);

        binder.postProcessAfterInitialization(factory, "factory");

        assertThat(factory.getAdviceChain()).containsExactly(existing, contextAdvice);
    }

    @Test
    @DisplayName("非 SimpleRabbitListenerContainerFactory 的 Bean 原样返回，不做处理")
    void ignoresNonTargetBean() {
        Object other = new Object();

        Object returned = binder.postProcessAfterInitialization(other, "other");

        assertThat(returned).isSameAs(other);
    }

    /** 占位 advice，仅用于验证合并保留既有元素 */
    static class NoOpAdvice implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) {
            return null;
        }
    }
}
