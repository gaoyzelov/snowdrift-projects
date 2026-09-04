package com.snowdrift.framework.mq.rocketmq.service;

import com.snowdrift.framework.mq.AbstractMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.rocketmq.support.RocketDelayLevels;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * RocketMQ 消息服务实现 — 基于原生 {@link RocketMQTemplate}
 * <p>
 * 同步/异步/延迟发送均走 RocketMQ 原生 producer：
 * — {@code topic} 直接为 RocketMQ topic（tag 可选：可用消息头 {@code RocketMQHeaders.TAGS} 或 destination {@code topic:tag}）；
 * — key 经 {@code RocketMQHeaders.KEYS} 映射为 RocketMQ keys（分片/去重键）；
 * — 延迟使用 RocketMQ 原生延迟级别（1~18），见 {@link RocketDelayLevels}。
 * 消费请使用原生 {@code @RocketMQMessageListener}；上下文头由 RocketMQTemplate 写入消息 properties，
 * 监听参数取 {@code org.apache.rocketmq.common.message.MessageExt} 后可用
 * {@code MqContextPropagator.restore(ext.getProperties())} 恢复。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RocketMqServiceImpl extends AbstractMqService {

    /** 默认发送超时（毫秒） */
    private static final long DEFAULT_SEND_TIMEOUT = 3000L;

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMqServiceImpl(RocketMQTemplate rocketMQTemplate,
                               Executor mqAsyncExecutor,
                               MqMessageConverter converter,
                               MqInterceptorRegistry interceptorRegistry,
                               MqContextPropagator contextPropagator) {
        super(mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
        SendResult sendResult = rocketMQTemplate.syncSend(topic, buildMessage(key, body, headers), DEFAULT_SEND_TIMEOUT);
        return toResult(sendResult);
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        validateSendArgs(topic, payload);
        validateDelay(delay);
        fireBeforeSend(topic, key, payload);
        try {
            byte[] body = converter.serialize(payload);
            Map<String, String> effectiveHeaders = buildHeaders(key, headers);
            Message<byte[]> message = buildMessage(key, body, effectiveHeaders);
            int delayLevel = RocketDelayLevels.map(delay);
            SendResult sendResult = rocketMQTemplate.syncSend(topic, message, DEFAULT_SEND_TIMEOUT, delayLevel);
            MqSendResult result = toResult(sendResult);
            fireAfterSend(topic, result);
            return result;
        } catch (RuntimeException e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload, Map<String, String> headers) {
        validateSendArgs(topic, payload);
        fireBeforeSend(topic, key, payload);
        try {
            byte[] body = converter.serialize(payload);
            Map<String, String> effectiveHeaders = buildHeaders(key, headers);
            Message<byte[]> message = buildMessage(key, body, effectiveHeaders);

            CompletableFuture<MqSendResult> future = new CompletableFuture<>();
            rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    MqSendResult result = toResult(sendResult);
                    fireAfterSend(topic, result);
                    future.complete(result);
                }

                @Override
                public void onException(Throwable throwable) {
                    fireOnSendError(topic, throwable);
                    future.completeExceptionally(throwable);
                }
            }, DEFAULT_SEND_TIMEOUT);
            return future;
        } catch (RuntimeException e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    // ========== 组装与映射 ==========

    /**
     * 构建 spring-messaging Message：payload 为已序列化字节；key 映射 RocketMQ keys；字符串头逐个写入
     */
    private Message<byte[]> buildMessage(String key, byte[] body, Map<String, String> headers) {
        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(body);
        if (StringUtils.isNotBlank(key)) {
            builder.setHeader(RocketMQHeaders.KEYS, key);
        }
        if (headers != null) {
            headers.forEach((headerKey, value) -> {
                if (headerKey != null && value != null) {
                    builder.setHeader(headerKey, value);
                }
            });
        }
        return builder.build();
    }

    private MqSendResult toResult(SendResult sendResult) {
        org.apache.rocketmq.common.message.MessageQueue queue = sendResult.getMessageQueue();
        String topic = queue != null ? queue.getTopic() : null;
        String queueId = queue != null ? String.valueOf(queue.getQueueId()) : null;
        return MqSendResult.builder()
                .topic(topic)
                .messageId(sendResult.getMsgId())
                .partitionOrQueue(queueId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
