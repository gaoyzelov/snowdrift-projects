package com.snowdrift.framework.mq.core;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 默认消息发送模板 — 基于 Spring Cloud Stream {@link StreamBridge}
 * <p>
 * 提供消息发送的核心实现：序列化、上下文注入、StreamBridge 调用。
 * 延迟发送默认为不支持，由各 binder 实现模块覆盖。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class DefaultMqTemplate implements IMqTemplate {

    protected final StreamBridge streamBridge;
    protected final MqProperties properties;

    public DefaultMqTemplate(StreamBridge streamBridge, MqProperties properties) {
        this.streamBridge = streamBridge;
        this.properties = properties;
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
    @SuppressWarnings("unchecked")
    public <T> MqSendResult send(String topic, String key, T payload, Map<String, String> headers) {
        byte[] bytes = JSON.toJSONBytes(payload);
        String originalType = payload.getClass().getName();

        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes)
                .setHeader(MqContextPropagator.HEADER_ORIGINAL_TYPE, originalType);

        if (StringUtils.isNotBlank(key)) {
            builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
        }

        // 注入 TTL 上下文（traceId, userId, tenantId, username）
        MqContextPropagator.inject(builder);

        // 注入用户自定义 headers
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::setHeader);
        }

        Message<byte[]> message = builder.build();
        boolean success = streamBridge.send(topic, message);
        if (!success) {
            log.error("消息发送失败: topic={}, key={}, type={}", topic, key, originalType);
            throw new MqException("mq.send.failed", new Object[]{topic});
        }

        log.debug("消息发送成功: topic={}, key={}, type={}", topic, key, originalType);
        return MqSendResult.builder()
                .topic(topic)
                .timestamp(System.currentTimeMillis())
                .build();
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(topic, key, payload, headers);
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("异步发送异常: topic={}, key={}", topic, key, e);
                throw ExceptionUtils.<RuntimeException>rethrow(e);
            }
        });
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
        throw new UnsupportedOperationException("当前 MQ 不支持延迟消息，请使用各 binder 模块（如 snowdrift-mq-rocketmq）或直接使用 StreamBridge");
    }
}
