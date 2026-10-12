package com.snowdrift.framework.mq.rocketmq.service;

import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.properties.RocketMqProperties;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RocketMqServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RocketMQ 发送实现黑盒测试：结果映射、key→KEYS、延迟级别、原生异步回调与异常包装
 * @since 1.0.0
 */
class RocketMqServiceImplTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
    private final SendResult sendResult = mock(SendResult.class);

    @BeforeEach
    void setUp() {
        when(sendResult.getMsgId()).thenReturn("msg-1");
        when(sendResult.getMessageQueue()).thenReturn(new MessageQueue("order", "broker-a", 7));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        MDC.clear();
    }

    private RocketMqServiceImpl newService() {
        return new RocketMqServiceImpl(rocketMQTemplate, new RocketMqProperties(), DIRECT_EXECUTOR,
                new FastJson2MqMessageConverter(), new MqInterceptorRegistry(),
                new MqContextPropagator(mqProperties()));
    }

    @Test
    @DisplayName("同步发送成功：messageId=msgId，partitionOrQueue=queueId，topic 取自 MessageQueue")
    void syncSendMapsResult() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(sendResult);
        RocketMqServiceImpl service = newService();

        MqSendResult result = service.send("order", "k1", "payload");

        assertThat(result.getMessageId()).isEqualTo("msg-1");
        assertThat(result.getPartitionOrQueue()).isEqualTo("7");
        assertThat(result.getTopic()).isEqualTo("order");
    }

    @Test
    @DisplayName("同步发送：key 映射为 RocketMQ KEYS 头")
    void syncSendMapsKeyToKeysHeader() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(sendResult);
        RocketMqServiceImpl service = newService();

        service.send("order", "shard-1", "payload");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("order"), captor.capture(), anyLong());
        assertThat(captor.getValue().getHeaders().get(RocketMQHeaders.KEYS)).isEqualTo("shard-1");
    }

    @Test
    @DisplayName("同步发送失败：包装为 MqException 并保留原因")
    void syncSendFailureWrapped() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong()))
                .thenThrow(new IllegalStateException("no route"));
        RocketMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.send("order", "payload"))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("RocketMQ 消息发送失败")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("边界：MessageQueue 为 null 时 topic/queueId 降级为 null，不抛异常")
    void nullMessageQueueDegrades() {
        when(sendResult.getMessageQueue()).thenReturn(null);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(sendResult);
        RocketMqServiceImpl service = newService();

        MqSendResult result = service.send("order", "payload");

        assertThat(result.getMessageId()).isEqualTo("msg-1");
        assertThat(result.getTopic()).isNull();
        assertThat(result.getPartitionOrQueue()).isNull();
    }

    @Test
    @DisplayName("延迟发送：按 Duration 映射为原生延迟级别并走带 delayLevel 的 syncSend")
    void sendDelayUsesNativeLevel() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong(), anyInt())).thenReturn(sendResult);
        RocketMqServiceImpl service = newService();

        MqSendResult result = service.sendDelay("order", "k", "payload", Duration.ofSeconds(5));

        assertThat(result.getMessageId()).isEqualTo("msg-1");
        // 5s → 级别 2
        verify(rocketMQTemplate).syncSend(eq("order"), any(Message.class), anyLong(), eq(2));
    }

    @Test
    @DisplayName("延迟发送：时长非法（null/0/负）抛 MqException，不触发发送")
    void sendDelayValidatesDuration() {
        RocketMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.sendDelay("order", "payload", Duration.ZERO))
                .isInstanceOf(MqException.class);
        verify(rocketMQTemplate, org.mockito.Mockito.never())
                .syncSend(anyString(), any(Message.class), anyLong(), anyInt());
    }

    @Test
    @DisplayName("原生异步发送成功：回调 onSuccess 后 future 完成")
    void asyncSendSuccess() {
        RocketMqServiceImpl service = newService();

        CompletableFuture<MqSendResult> future = service.sendAsync("order", "k", "payload");

        ArgumentCaptor<SendCallback> captor = ArgumentCaptor.forClass(SendCallback.class);
        verify(rocketMQTemplate).asyncSend(eq("order"), any(Message.class), captor.capture(), anyLong());
        captor.getValue().onSuccess(sendResult);

        assertThat(future.join().getMessageId()).isEqualTo("msg-1");
    }

    @Test
    @DisplayName("原生异步发送失败：回调 onException 后 future 以 MqException 异常完成")
    void asyncSendFailure() {
        RocketMqServiceImpl service = newService();

        CompletableFuture<MqSendResult> future = service.sendAsync("order", "k", "payload");

        ArgumentCaptor<SendCallback> captor = ArgumentCaptor.forClass(SendCallback.class);
        verify(rocketMQTemplate).asyncSend(eq("order"), any(Message.class), captor.capture(), anyLong());
        captor.getValue().onException(new IllegalStateException("timeout"));

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join).hasCauseInstanceOf(MqException.class);
    }

    /** 默认配置已开启消息签名，发送/消费流水线测试提供测试密钥 */
    private static MqProperties mqProperties() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        return properties;
    }
}
