package com.snowdrift.framework.mq.kafka.core;

import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.Duration;
import java.util.Map;

/**
 * Kafka 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream Kafka Binder。
 * 延迟消息 Kafka 原生不支持，降级为即时发送并输出 WARN 日志。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class KafkaMqTemplate extends DefaultMqTemplate {

    public KafkaMqTemplate(StreamBridge streamBridge, MqProperties properties) {
        super(streamBridge, properties);
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        log.warn("Kafka 不支持原生延迟消息，降级为即时发送。topic={}, delay={}", topic, delay);
        return send(topic, key, payload, headers);
    }
}
