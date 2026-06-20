package com.snowdrift.framework.mq.rabbitmq.core;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.config.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * RabbitMQ 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream RabbitMQ Binder。
 * 延迟消息支持两种模式：
 * — delayPluginEnabled=true：使用 rabbitmq-delayed-message-exchange 插件的 x-delay header
 * — delayPluginEnabled=false：使用 x-message-ttl，需要 exchange 预配置 DLX
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RabbitMqTemplate extends DefaultMqTemplate {

    private final RabbitMqProperties rabbitProperties;

    public RabbitMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                            RabbitMqProperties rabbitProperties) {
        super(streamBridge, mqProperties);
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        byte[] bytes = JSON.toJSONBytes(payload);
        String originalType = payload.getClass().getName();

        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes)
                .setHeader(MqContextPropagator.HEADER_ORIGINAL_TYPE, originalType);

        if (Boolean.TRUE.equals(rabbitProperties.getDelayPluginEnabled())) {
            // rabbitmq-delayed-message-exchange 插件: x-delay header（毫秒）
            builder.setHeader("x-delay", delay.toMillis());
            log.debug("RabbitMQ 延迟消息（x-delay 插件）: topic={}, delay={}ms", topic, delay.toMillis());
        } else {
            // 降级方案：x-message-ttl + DLX（exchange 需预配置死信队列）
            builder.setHeader("x-message-ttl", delay.toMillis());
            log.debug("RabbitMQ 延迟消息（x-message-ttl + DLX）: topic={}, ttl={}ms，请确保 exchange 已配置 DLX",
                    topic, delay.toMillis());
        }

        if (StringUtils.isNotBlank(key)) {
            builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
        }

        // 注入 TTL 上下文
        MqContextPropagator.inject(builder);

        // 注入用户自定义 headers
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::setHeader);
        }

        Message<byte[]> message = builder.build();
        boolean success = streamBridge.send(topic, message);
        if (!success) {
            log.error("RabbitMQ 延迟消息发送失败: topic={}, delay={}", topic, delay);
            throw new MqException("mq.send.failed", new Object[]{topic});
        }

        log.debug("RabbitMQ 延迟消息发送成功: topic={}, delay={}", topic, delay);
        return MqSendResult.builder()
                .topic(topic)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
