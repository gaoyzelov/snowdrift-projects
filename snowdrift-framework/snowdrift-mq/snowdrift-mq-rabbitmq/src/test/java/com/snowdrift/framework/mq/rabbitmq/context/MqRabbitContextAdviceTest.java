package com.snowdrift.framework.mq.rabbitmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.exception.MqException;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MqRabbitContextAdviceTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RabbitMQ 容器级上下文 Advice 黑盒测试：有消息则恢复→调用→清理，无消息仅调用+清理，异常与签名失败语义
 * @since 1.0.0
 */
class MqRabbitContextAdviceTest {

    private final MqContextPropagator propagator = mock(MqContextPropagator.class);
    private final MqRabbitContextAdvice advice = new MqRabbitContextAdvice(propagator);

    private Message messageWithHeaders() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-snowdrift-trace-id", "trace-1");
        return new Message("body".getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    @DisplayName("含 Message 参数：从消息头恢复上下文→调用业务→finally 清理")
    @SuppressWarnings("unchecked")
    void restoresProceedsAndClears() throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{"payload", messageWithHeaders()});
        when(invocation.proceed()).thenReturn("ok");

        Object result = advice.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(propagator).restore(captor.capture());
        assertThat(captor.getValue()).containsEntry("x-snowdrift-trace-id", "trace-1");
        verify(propagator).clear();
    }

    @Test
    @DisplayName("无 Message 参数：不恢复上下文，但仍调用业务并清理")
    void noMessageArgSkipsRestore() throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{"just-a-string"});
        when(invocation.proceed()).thenReturn("ok");

        Object result = advice.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        verify(propagator, never()).restore(any());
        verify(propagator).clear();
    }

    @Test
    @DisplayName("业务抛异常：异常照常上抛，finally 仍清理上下文")
    void businessFailureStillClears() throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{messageWithHeaders()});
        when(invocation.proceed()).thenThrow(new IllegalStateException("业务失败"));

        assertThatThrownBy(() -> advice.invoke(invocation))
                .isInstanceOf(IllegalStateException.class);
        verify(propagator).clear();
    }

    @Test
    @DisplayName("签名校验失败：restore 抛异常则跳过业务调用并上抛")
    void signatureFailureAborts() throws Throwable {
        doThrow(new MqException("消息签名校验失败，已拒绝消费该消息")).when(propagator).restore(any());
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{messageWithHeaders()});

        assertThatThrownBy(() -> advice.invoke(invocation))
                .isInstanceOf(MqException.class);
        verify(invocation, never()).proceed();
    }
}
