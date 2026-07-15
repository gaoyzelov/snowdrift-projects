package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 默认消息发送模板 — 基于 Spring Cloud Stream {@link StreamBridge}
 * <p>
 * 提供消息发送的核心实现：序列化、上下文注入、StreamBridge 调用。
 * 延迟发送默认为不支持，由各 binder 实现模块覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class DefaultMqServiceImpl implements IMqService {

    protected final StreamBridge streamBridge;
    protected final MqProperties properties;
    protected final Executor mqAsyncExecutor;
    protected final MqMessageConverter converter;
    protected final MqInterceptorRegistry interceptorRegistry;
    protected final MqContextPropagator contextPropagator;


    public DefaultMqServiceImpl(StreamBridge streamBridge, MqProperties properties,
                                Executor mqAsyncExecutor, MqMessageConverter converter,
                                MqInterceptorRegistry interceptorRegistry,MqContextPropagator contextPropagator) {
        this.streamBridge = streamBridge;
        this.properties = properties;
        this.mqAsyncExecutor = mqAsyncExecutor;
        this.converter = converter;
        this.contextPropagator = contextPropagator;
        this.interceptorRegistry = interceptorRegistry;
    }

    // ========== 同步发送 ==========

    @Override
    public <T> MqSendResult send(String topic, T payload) {
        return send(topic, null, payload, null);
    }

    @Override
    public <T> MqSendResult send(String topic, String key, T payload) {
        return send(topic, key, payload, null);
    }

    @Override
    public <T> MqSendResult send(String topic, String key, T payload, Map<String, String> headers) {
        long start = System.currentTimeMillis();
        fireBeforeSend(topic, key, payload);

        try {
            String originalType = payload.getClass().getName();
            Message<byte[]> message = buildMessage(key, payload, headers);
            boolean success = streamBridge.send(topic, message);
            long elapsed = System.currentTimeMillis() - start;

            if (!success) {
                MqException ex = new MqException("mq.send.failed", new Object[]{topic});
                log.warn("消息发送失败: topic={}, key={}, type={}", topic, key, originalType);
                throw ex;
            }

            MqSendResult result = MqSendResult.builder()
                    .topic(topic)
                    .timestamp(System.currentTimeMillis())
                    .build();

            fireAfterSend(topic, result);

            log.debug("消息发送成功: topic={}, key={}, type={}, elapsed={}ms", topic, key, originalType, elapsed);
            return result;

        } catch (Exception e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    // ========== 拦截器辅助方法（供子类 sendDelay 等调用） ==========

    /**
     * 触发发送前拦截器
     */
    protected void fireBeforeSend(String topic, String key, Object payload) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.beforeSend(topic, key, payload);
            } catch (Exception e) {
                log.warn("拦截器 beforeSend 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    /**
     * 触发发送后拦截器
     */
    protected void fireAfterSend(String topic, MqSendResult result) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.afterSend(topic, result);
            } catch (Exception e) {
                log.warn("拦截器 afterSend 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    /**
     * 触发发送异常拦截器
     */
    protected void fireOnSendError(String topic, Throwable ex) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.onSendError(topic, ex);
            } catch (Exception e) {
                log.warn("拦截器 onSendError 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    // ========== 异步发送 ==========

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, T payload) {
        return sendAsync(topic, null, payload, null);
    }

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload) {
        return sendAsync(topic, key, payload, null);
    }

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload, Map<String, String> headers) {
        final String traceId = MDC.get(MqContextPropagator.TRACE_ID_KEY);
        final SecurityContext context = SecurityContextHolder.getContext();
        return CompletableFuture.supplyAsync(() -> {
            MDC.put(MqContextPropagator.TRACE_ID_KEY, traceId);
            SecurityContextHolder.setContext(context);
            try {
                return send(topic, key, payload, headers);
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("异步发送异常: topic={}, key={}", topic, key, e);
                throw ExceptionUtils.<RuntimeException>rethrow(e);
            }finally {
                MDC.clear();
                SecurityContextHolder.clear();
            }
        }, mqAsyncExecutor);
    }

    // ========== 延迟发送（默认不支持，由子类覆盖） ==========

    @Override
    public <T> MqSendResult sendDelay(String topic, T payload, Duration delay) {
        return sendDelay(topic, null, payload, delay, null);
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay) {
        return sendDelay(topic, key, payload, delay, null);
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        throw new MqException("mq.send.delay.unsupported");
    }

    // ========== 延迟发送模板方法（供子类复用） ==========

    /**
     * 延迟发送模板方法 — 子类只需提供延迟头部的设置逻辑
     * <p>统一处理：Duration 空值校验 → 拦截器触发 → 序列化 → 上下文注入 → StreamBridge 发送</p>
     *
     * @param topic              目标 topic
     * @param key                消息 Key（可空）
     * @param payload            消息体
     * @param delay              延迟时长（不可为空）
     * @param headers            自定义消息头（可空）
     * @param delayHeaderSetter  延迟头部设置回调
     * @param <T>                消息体类型
     * @return 发送结果
     */
    protected <T> MqSendResult doSendDelay(String topic, String key, T payload,
                                            Duration delay, Map<String, String> headers,
                                            java.util.function.Consumer<MessageBuilder<byte[]>> delayHeaderSetter) {
        if (delay == null) {
            throw new MqException("mq.send.delay.null");
        }
        fireBeforeSend(topic, key, payload);
        try {
            byte[] bytes = converter.serialize(payload);
            MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes);
            delayHeaderSetter.accept(builder);

            if (StringUtils.isNotBlank(key)) {
                builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
            }
            contextPropagator.inject(builder);
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(builder::setHeader);
            }

            Message<byte[]> message = builder.build();
            boolean success = streamBridge.send(topic, message);
            if (!success) {
                MqException ex = new MqException("mq.send.failed", new Object[]{topic});
                log.warn("延迟消息发送失败: topic={}, delay={}", topic, delay);
                throw ex;
            }

            MqSendResult result = MqSendResult.builder().topic(topic)
                    .timestamp(System.currentTimeMillis()).build();
            fireAfterSend(topic, result);
            log.debug("延迟消息发送成功: topic={}, delay={}", topic, delay);
            return result;
        } catch (Exception e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    // ========== 批量发送 ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<MqSendResult> results = new ArrayList<>(messages.size());
        List<Exception> errors = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            MqMessage<T> msg = messages.get(i);
            try {
                results.add(send(topic, msg.getKey(), msg.getPayload(), msg.getHeaders()));
            } catch (Exception e) {
                log.error("批量发送第 {} 条失败: topic={}", i, topic, e);
                errors.add(e);
                results.add(null);
            }
        }
        if (!errors.isEmpty()) {
            throw new MqException("mq.send.batch.partial-failed",
                    new Object[]{topic, results.stream().filter(Objects::nonNull).count(), messages.size()});
        }
        return results;
    }

    // ========== 消息构建（供子类复用） ==========

    /**
     * 构建 Spring Message，包含序列化、上下文注入和自定义头部
     * <p>供子类批量/延迟路径复用，确保拦截器和上下文传播的一致性</p>
     *
     * @param key     消息 Key（可空）
     * @param payload 消息体
     * @param headers 自定义消息头（可空）
     * @return 构建完成的 Spring Message
     */
    protected Message<byte[]> buildMessage(String key, Object payload,
                                            Map<String, String> headers) {
        byte[] bytes = converter.serialize(payload);
        return buildMessageFromBytes(key, bytes, headers);
    }

    /**
     * 从已序列化的字节数组构建 Spring Message（含上下文注入和自定义头部）
     * <p>供子类在己方已完成序列化的场景下复用，避免二次序列化</p>
     *
     * @param key     消息 Key（可空）
     * @param bytes   已序列化的消息体字节
     * @param headers 自定义消息头（可空）
     * @return 构建完成的 Spring Message
     */
    protected Message<byte[]> buildMessageFromBytes(String key, byte[] bytes,
                                                      Map<String, String> headers) {
        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes);

        if (StringUtils.isNotBlank(key)) {
            builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
        }

        // 注入 TTL 上下文（traceId, userId, tenantId, username）
        contextPropagator.inject(builder);

        // 注入用户自定义 headers
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::setHeader);
        }

        return builder.build();
    }
}
