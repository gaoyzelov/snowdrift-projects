package com.snowdrift.framework.mq.rocketmq.service;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.AbstractMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.rocketmq.properties.RocketMqProperties;
import com.snowdrift.framework.mq.rocketmq.support.RocketDelayLevels;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.MDC;
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

    private final RocketMQTemplate rocketMQTemplate;
    private final long sendTimeoutMillis;

    public RocketMqServiceImpl(RocketMQTemplate rocketMQTemplate,
                               RocketMqProperties rocketMqProperties,
                               Executor mqAsyncExecutor,
                               MqMessageConverter converter,
                               MqInterceptorRegistry interceptorRegistry,
                               MqContextPropagator contextPropagator) {
        super(mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
        this.rocketMQTemplate = rocketMQTemplate;
        this.sendTimeoutMillis = rocketMqProperties.getSendTimeout().toMillis();
    }

    @Override
    protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(topic, buildMessage(key, body, headers), sendTimeoutMillis);
            return toResult(sendResult);
        } catch (RuntimeException e) {
            throw toSendException(topic, e);
        }
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
            MqSendResult result;
            try {
                SendResult sendResult = rocketMQTemplate.syncSend(topic, message, sendTimeoutMillis, delayLevel);
                result = toResult(sendResult);
            } catch (RuntimeException e) {
                throw toSendException(topic, e);
            }
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
        // 捕获调用方线程上下文，供 broker I/O 线程回调内恢复（与基类 executor 包装路径语义一致）
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        SecurityContext callerSecurity = SecurityContextHolder.peekContext();
        try {
            byte[] body = converter.serialize(payload);
            Map<String, String> effectiveHeaders = buildHeaders(key, headers);
            Message<byte[]> message = buildMessage(key, body, effectiveHeaders);

            CompletableFuture<MqSendResult> future = new CompletableFuture<>();
            rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    replayContext(callerMdc, callerSecurity);
                    try {
                        MqSendResult result = toResult(sendResult);
                        fireAfterSend(topic, result);
                        future.complete(result);
                    } finally {
                        SecurityContextHolder.clear();
                        MDC.clear();
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    replayContext(callerMdc, callerSecurity);
                    try {
                        MqException mqException = toSendException(topic, throwable);
                        fireOnSendError(topic, mqException);
                        future.completeExceptionally(mqException);
                    } finally {
                        SecurityContextHolder.clear();
                        MDC.clear();
                    }
                }
            }, sendTimeoutMillis);
            return future;
        } catch (RuntimeException e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    // ========== 组装与映射 ==========

    /**
     * 统一转换为 MqException：已是 MqException 直接透传，否则包装并保留原始 cause
     */
    private static MqException toSendException(String topic, Throwable cause) {
        if (cause instanceof MqException mqException) {
            return mqException;
        }
        return new MqException("RocketMQ 消息发送失败: topic=" + topic + "，原因=" + describe(cause), cause);
    }

    /**
     * 摘要发送异常原因（message 为空时回退到异常类型名）
     */
    private static String describe(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

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
