package com.snowdrift.framework.mq.rabbitmq.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.context.MqRabbitContextAdvice;
import com.snowdrift.framework.mq.rabbitmq.context.MqRabbitListenerContextBinder;
import com.snowdrift.framework.mq.rabbitmq.properties.RabbitMqProperties;
import com.snowdrift.framework.mq.rabbitmq.service.RabbitMqServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftRabbitMqConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RabbitMQ 自动配置装配黑盒测试：IMqService 实现与消费端上下文 Advice/绑定器均可正确创建
 * @since 1.0.0
 */
class SnowdriftRabbitMqConfigurationTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final SnowdriftRabbitMqConfiguration configuration = new SnowdriftRabbitMqConfiguration();
    private final MqContextPropagator propagator = new MqContextPropagator(mqProperties());

    @Test
    @DisplayName("注册 IMqService 的 RabbitMQ 实现")
    void registersRabbitMqService() {
        RabbitMqServiceImpl service = configuration.rabbitMqService(mock(RabbitTemplate.class),
                new RabbitMqProperties(), DIRECT_EXECUTOR, new FastJson2MqMessageConverter(),
                new MqInterceptorRegistry(), propagator);

        assertThat(service).isInstanceOf(IMqService.class);
    }

    @Test
    @DisplayName("装配消费端上下文 Advice 与容器绑定器")
    void wiresConsumerContextBeans() {
        MqRabbitContextAdvice advice = configuration.mqRabbitContextAdvice(propagator);

        MqRabbitListenerContextBinder binder = configuration.mqRabbitListenerContextBinder(advice);

        assertThat(advice).isNotNull();
        assertThat(binder).isInstanceOf(MqRabbitListenerContextBinder.class);
    }

    /** 默认配置已开启消息签名，发送/消费流水线测试提供测试密钥 */
    private static MqProperties mqProperties() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        return properties;
    }
}
