package com.snowdrift.framework.mq.kafka.service;

import com.snowdrift.framework.mq.core.DefaultMqServiceImpl;
import com.snowdrift.framework.mq.core.MqInterceptorRegistry;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Kafka 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream Kafka Binder。
 * 批量发送优先使用 {@link KafkaTemplate}（如可用），Kafka Producer 内部按 linger.ms 自动批次发送。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class KafkaMqServiceImpl extends DefaultMqServiceImpl implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private volatile KafkaTemplate<byte[], byte[]> kafkaTemplate;

    public KafkaMqServiceImpl(StreamBridge streamBridge, MqProperties properties,
                              Executor mqAsyncExecutor, MqMessageConverter converter,
                              MqInterceptorRegistry interceptorRegistry) {
        super(streamBridge, properties, mqAsyncExecutor, converter, interceptorRegistry);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        log.warn("Kafka 不支持原生延迟消息，降级为即时发送。topic={}, delay={}", topic, delay);
        return send(topic, key, payload, headers);
    }

    // ========== 批量发送（KafkaTemplate，Producer 内部自动批次） ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        KafkaTemplate<byte[], byte[]> template = getKafkaTemplate();
        if (template != null) {
            return sendBatchWithKafkaTemplate(topic, messages, template);
        }

        // 回退到逐条 StreamBridge 循环（Kafka Producer 仍会按 linger.ms 内部批次发送）
        log.debug("KafkaTemplate 不可用，使用 StreamBridge 循环批量发送");
        return super.sendBatch(topic, messages);
    }

    private <T> List<MqSendResult> sendBatchWithKafkaTemplate(String topic,
                                                               List<MqMessage<T>> messages,
                                                               KafkaTemplate<byte[], byte[]> template) {
        // 逐条：触发 beforeSend 拦截器 → 构建消息 → 提交 Kafka send
        List<java.util.concurrent.Future<RecordMetadata>> futures = new ArrayList<>(messages.size());
        for (MqMessage<T> mqMsg : messages) {
            fireBeforeSend(topic, mqMsg.getKey(), mqMsg.getPayload());

            // 使用 buildMessage 统一构建（包含上下文注入和自定义头部）
            Message<byte[]> springMsg = buildMessage(topic, mqMsg.getKey(),
                    mqMsg.getPayload(), mqMsg.getHeaders());

            byte[] msgKey = mqMsg.getKey() != null
                    ? mqMsg.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8) : null;
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, null, msgKey, springMsg.getPayload());

            // 将 Spring Message headers 写入 Kafka Record headers
            springMsg.getHeaders().forEach(header -> {
                byte[] headerValue = header.getValue() instanceof byte[]
                        ? (byte[]) header.getValue()
                        : header.getValue().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                record.headers().add(header.getKey(), headerValue);
            });

            futures.add(template.send(record).thenApply(SendResult::getRecordMetadata));
        }

        // 逐条等待结果，收集成功/失败信息（不因单条失败丢弃其余结果）
        List<MqSendResult> results = new ArrayList<>(messages.size());
        List<Exception> errors = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                RecordMetadata meta = futures.get(i).get();
                MqSendResult result = MqSendResult.builder()
                        .messageId(topic + "-" + meta.partition() + "-" + meta.offset())
                        .topic(topic)
                        .partitionOrQueue(String.valueOf(meta.partition()))
                        .timestamp(meta.timestamp())
                        .build();
                results.add(result);
                fireAfterSend(topic, result);
            } catch (Exception e) {
                log.error("Kafka 批量发送第 {} 条失败: topic={}", i, topic, e);
                fireOnSendError(topic, e);
                errors.add(e);
                results.add(null); // 占位，保持索引对齐
            }
        }

        if (!errors.isEmpty()) {
            long successCount = results.stream().filter(java.util.Objects::nonNull).count();
            throw new MqException("mq.send.batch.partial-failed",
                    new Object[]{topic, successCount, messages.size()});
        }
        log.debug("Kafka 批量发送完成: topic={}, count={}", topic, messages.size());
        return results;
    }

    private KafkaTemplate<byte[], byte[]> getKafkaTemplate() {
        if (this.kafkaTemplate == null) {
            synchronized (this) {
                if (this.kafkaTemplate == null) {
                    try {
                        //noinspection unchecked
                        this.kafkaTemplate = applicationContext.getBean(KafkaTemplate.class);
                    } catch (Exception e) {
                        log.debug("KafkaTemplate Bean 不存在，批量发送将使用 StreamBridge 循环");
                    }
                }
            }
        }
        return this.kafkaTemplate;
    }
}
