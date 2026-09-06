package com.snowdrift.framework.mq.kafka.service;

import com.snowdrift.framework.mq.AbstractMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.kafka.context.KafkaHeaderCodec;
import com.snowdrift.framework.mq.model.MqSendResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Kafka 消息服务实现 — 基于原生 {@link KafkaTemplate}
 * <p>
 * 同步/异步发送均走 Kafka 原生 producer，可拿到真实 {@code topic-partition-offset} 等元数据；
 * key 作为 {@link ProducerRecord} 分区键参与分区路由。
 * Kafka 无原生延迟消息能力，{@link #sendDelay} 继承基类默认：抛 {@link UnsupportedOperationException}。
 * 消费请直接使用原生 {@code @KafkaListener}，框架以 {@code RecordInterceptor} 自动恢复上下文。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class KafkaMqServiceImpl extends AbstractMqService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaMqServiceImpl(KafkaTemplate<String, byte[]> kafkaTemplate,
                              Executor mqAsyncExecutor,
                              MqMessageConverter converter,
                              MqInterceptorRegistry interceptorRegistry,
                              MqContextPropagator contextPropagator) {
        super(mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
        this.kafkaTemplate = kafkaTemplate;
    }

    // ========== 同步发送（原生，阻塞等待 broker ack） ==========

    @Override
    protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
        ProducerRecord<String, byte[]> record = buildRecord(topic, key, body, headers);
        try {
            SendResult<String, byte[]> sendResult = kafkaTemplate.send(record).join();
            return toResult(sendResult);
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new MqException("Kafka 消息发送失败: topic=" + topic + "，msg=" + cause.getMessage());
        }
    }

    // ========== 异步发送（Kafka 原生异步，非线程池包装） ==========

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload, Map<String, String> headers) {
        validateSendArgs(topic, payload);
        fireBeforeSend(topic, key, payload);
        try {
            byte[] body = converter.serialize(payload);
            Map<String, String> effectiveHeaders = buildHeaders(key, headers);
            ProducerRecord<String, byte[]> record = buildRecord(topic, key, body, effectiveHeaders);
            return kafkaTemplate.send(record).handle((sendResult, ex) -> {
                if (ex != null) {
                    Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
                    fireOnSendError(topic, cause);
                    throw cause instanceof RuntimeException runtimeException
                            ? runtimeException
                            : new MqException("Kafka 消息发送失败: topic=" + topic + "，msg=" + cause.getMessage());
                }
                MqSendResult result = toResult(sendResult);
                fireAfterSend(topic, result);
                return result;
            });
        } catch (RuntimeException e) {
            fireOnSendError(topic, e);
            throw e;
        }
    }

    // ========== 组装与映射 ==========

    private ProducerRecord<String, byte[]> buildRecord(String topic, String key, byte[] body, Map<String, String> headers) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, body);
        KafkaHeaderCodec.apply(headers, record);
        return record;
    }

    /**
     * 把原生 SendResult 映射为统一 {@link MqSendResult}（messageId = topic-partition-offset）
     */
    private MqSendResult toResult(SendResult<String, byte[]> sendResult) {
        RecordMetadata metadata = sendResult.getRecordMetadata();
        long timestamp = metadata.timestamp() >= 0 ? metadata.timestamp() : System.currentTimeMillis();
        return MqSendResult.builder()
                .topic(metadata.topic())
                .messageId(metadata.topic() + "-" + metadata.partition() + "-" + metadata.offset())
                .partitionOrQueue(String.valueOf(metadata.partition()))
                .timestamp(timestamp)
                .build();
    }
}
