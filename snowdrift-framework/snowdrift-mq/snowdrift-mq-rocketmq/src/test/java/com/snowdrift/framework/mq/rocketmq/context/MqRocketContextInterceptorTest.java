package com.snowdrift.framework.mq.rocketmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.exception.MqException;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MqRocketContextInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RocketMQ 容器级上下文拦截器单元测试：仅拦截 handleMessage、恢复→调用→清理时序与签名失败快速中止
 * @since 1.0.0
 */
class MqRocketContextInterceptorTest {

    // 仅用于提供名为 handleMessage / other 的 Method，拦截器只按方法名判定
    @SuppressWarnings("unused")
    void handleMessage(MessageExt messageExt) {
    }

    @SuppressWarnings("unused")
    void other() {
    }

    private Method method(String name, Class<?>... params) throws NoSuchMethodException {
        return MqRocketContextInterceptorTest.class.getDeclaredMethod(name, params);
    }

    @Test
    @DisplayName("handleMessage：先恢复上下文→调用业务→finally 清理")
    void restoresProceedsAndClears() throws Throwable {
        MqContextPropagator propagator = mock(MqContextPropagator.class);
        MqRocketContextInterceptor interceptor = new MqRocketContextInterceptor(propagator);
        MessageExt ext = new MessageExt();
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method("handleMessage", MessageExt.class));
        when(invocation.getArguments()).thenReturn(new Object[]{ext});
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        verify(propagator).restore(ext.getProperties());
        verify(invocation).proceed();
        verify(propagator).clear();
    }

    @Test
    @DisplayName("非 handleMessage 方法原样透传，不恢复/清理上下文")
    void passesThroughNonHandleMessage() throws Throwable {
        MqContextPropagator propagator = mock(MqContextPropagator.class);
        MqRocketContextInterceptor interceptor = new MqRocketContextInterceptor(propagator);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method("other"));
        when(invocation.proceed()).thenReturn("passed");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("passed");
        verify(propagator, never()).restore(any());
        verify(propagator, never()).clear();
    }

    @Test
    @DisplayName("签名校验失败：restore 抛异常则跳过业务调用，但 finally 仍清理上下文")
    void signatureFailureAbortsAndStillClears() throws Throwable {
        MqContextPropagator propagator = mock(MqContextPropagator.class);
        doThrow(new MqException("消息签名校验失败，已拒绝消费该消息")).when(propagator).restore(any());
        MqRocketContextInterceptor interceptor = new MqRocketContextInterceptor(propagator);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method("handleMessage", MessageExt.class));
        when(invocation.getArguments()).thenReturn(new Object[]{new MessageExt()});

        assertThatThrownBy(() -> interceptor.invoke(invocation))
                .isInstanceOf(MqException.class);

        verify(invocation, never()).proceed();
        verify(propagator).clear();
    }
}
