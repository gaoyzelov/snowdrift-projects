package com.snowdrift.framework.mq.rabbitmq.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * MqRabbitListenerContainerFactoryCoverageTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description binder 必须覆盖全部 RabbitMQ 容器工厂类型：只识别 Simple 时，
 *              用 Direct 工厂的 @RabbitListener 既不恢复也不清理上下文
 * @since 1.0.0
 */
class MqRabbitListenerContainerFactoryCoverageTest {

    private final MqRabbitContextAdvice advice = mock(MqRabbitContextAdvice.class);
    private final MqRabbitListenerContextBinder binder = new MqRabbitListenerContextBinder(advice);

    @Test
    @DisplayName("DirectRabbitListenerContainerFactory 也要挂上 Advice（此前只识别 Simple）")
    void directFactoryGetsAdvice() {
        DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();

        binder.postProcessAfterInitialization(factory, "directFactory");

        assertThat(factory.getAdviceChain()).containsExactly(advice);
    }

    @Test
    @DisplayName("SimpleRabbitListenerContainerFactory 仍然被覆盖（不回退既有行为）")
    void simpleFactoryGetsAdvice() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        binder.postProcessAfterInitialization(factory, "simpleFactory");

        assertThat(factory.getAdviceChain()).containsExactly(advice);
    }

    @Test
    @DisplayName("自定义工厂子类同样被覆盖：按共同父类判定，而非逐个枚举具体类型")
    void customSubclassGetsAdvice() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory() {
        };

        binder.postProcessAfterInitialization(factory, "customFactory");

        assertThat(factory.getAdviceChain()).containsExactly(advice);
    }
}
