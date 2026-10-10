package com.snowdrift.framework.mq.rabbitmq.service;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.properties.RabbitMqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * RabbitMqServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RabbitMQ 发送实现单元测试：exchange/routing-key 映射、延迟插件开关、x-delay 数值头与异常包装
 * @since 1.0.0
 */
class RabbitMqServiceImplTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private RabbitTemplate rabbitTemplate;
    private RabbitMqProperties rabbitProperties;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        rabbitProperties = new RabbitMqProperties();
    }

    private RabbitMqServiceImpl newService() {
        return new RabbitMqServiceImpl(rabbitTemplate, rabbitProperties, DIRECT_EXECUTOR,
                new FastJson2MqMessageConverter(), new MqInterceptorRegistry(),
                new MqContextPropagator(new MqProperties()));
    }

    @Test
    @DisplayName("同步发送：topic=exchange，key 缺省为空 routing key，返回自生成 messageId")
    void sendMapsExchangeAndDefaultRoutingKey() {
        RabbitMqServiceImpl service = newService();

        MqSendResult result = service.send("order-exchange", "payload");

        assertThat(result.getTopic()).isEqualTo("order-exchange");
        assertThat(result.getMessageId()).isNotBlank();
        assertThat(result.getPartitionOrQueue()).isEmpty();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("order-exchange"), eq(""), captor.capture());
        assertThat(captor.getValue().getMessageProperties().getMessageId()).isEqualTo(result.getMessageId());
    }

    @Test
    @DisplayName("带 key 发送：key 作为 routing key 透传")
    void sendUsesKeyAsRoutingKey() {
        RabbitMqServiceImpl service = newService();

        service.send("order-exchange", "rk-1", "payload");

        verify(rabbitTemplate).send(eq("order-exchange"), eq("rk-1"), any(Message.class));
    }

    @Test
    @DisplayName("原生发送抛异常时包装为 MqException 并保留原因")
    void sendFailureWrappedAsMqException() {
        doThrow(new IllegalStateException("channel closed"))
                .when(rabbitTemplate).send(any(), any(), any(Message.class));
        RabbitMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.send("exchange", "payload"))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("RabbitMQ 消息发送失败")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("未启用延迟插件时 sendDelay 抛 UnsupportedOperationException")
    void sendDelayRejectedWhenPluginDisabled() {
        rabbitProperties.setDelayPluginEnabled(false);
        RabbitMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.sendDelay("delay-exchange", "payload", Duration.ofSeconds(5)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("启用延迟插件时 sendDelay 以整数毫秒写入 x-delay（非字符串 longstr）")
    void sendDelaySetsNumericXDelayHeader() {
        rabbitProperties.setDelayPluginEnabled(true);
        RabbitMqServiceImpl service = newService();

        service.sendDelay("delay-exchange", "rk", "payload", Duration.ofSeconds(5), null);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("delay-exchange"), eq("rk"), captor.capture());
        assertThat(captor.getValue().getMessageProperties().getDelayLong()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("边界：x-delay 为非法数值时降级为普通字符串头，不设置 delayLong")
    void invalidXDelayFallsBackToStringHeader() {
        RabbitMqServiceImpl service = newService();

        service.send("exchange", "rk", "payload", java.util.Map.of("x-delay", "not-a-number"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("exchange"), eq("rk"), captor.capture());
        assertThat(captor.getValue().getMessageProperties().getDelayLong()).isNull();
        assertThat(captor.getValue().getMessageProperties().getHeaders())
                .containsEntry("x-delay", "not-a-number");
    }
}
