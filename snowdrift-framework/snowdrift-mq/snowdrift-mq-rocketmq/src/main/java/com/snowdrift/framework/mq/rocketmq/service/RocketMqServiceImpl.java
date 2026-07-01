package com.snowdrift.framework.mq.rocketmq.service;

import com.snowdrift.framework.mq.core.DefaultMqServiceImpl;
import com.snowdrift.framework.mq.core.MqInterceptorRegistry;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.config.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * RocketMQ 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream RocketMQ Binder。
 * 延迟消息使用 RocketMQ 原生延迟级别（1-18）。
 * 批量发送：若容器中存在 {@link DefaultMQProducer} Bean（如 rocketmq-spring-boot 提供），
 * 则使用原生 {@code send(Collection)} 单次网络请求；否则回退为 StreamBridge 循环。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RocketMqServiceImpl extends DefaultMqServiceImpl {

    private static final String ROCKETMQ_DELAY_LEVEL_HEADER = "DELAY";

    private static final long[] DELAY_LEVEL_SECONDS = {
        0, 1, 5, 10, 30, 60, 120, 180, 240, 300,
        360, 420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    private final ObjectProvider<DefaultMQProducer> batchProducerProvider;
    private final RocketMqProperties rocketProperties;
    private volatile DefaultMQProducer batchProducer;
    private volatile boolean batchProducerLookedUp;

    public RocketMqServiceImpl(StreamBridge streamBridge, MqProperties properties,
                               Executor mqAsyncExecutor, MqMessageConverter converter,
                               ObjectProvider<DefaultMQProducer> batchProducerProvider,
                               RocketMqProperties rocketProperties,
                               MqInterceptorRegistry interceptorRegistry) {
        super(streamBridge, properties, mqAsyncExecutor, converter, interceptorRegistry);
        this.batchProducerProvider = batchProducerProvider;
        this.rocketProperties = rocketProperties;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        int delayLevel = mapDurationToDelayLevel(delay);
        return doSendDelay(topic, key, payload, delay, headers, builder ->
                builder.setHeader(ROCKETMQ_DELAY_LEVEL_HEADER, delayLevel));
    }

    // ========== 批量发送 ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        DefaultMQProducer producer = getBatchProducer();
        if (producer != null) {
            return sendBatchWithProducer(topic, messages, producer);
        }

        // 回退：StreamBridge 循环发送，复用 SCS 管理的 Producer 连接
        log.debug("DefaultMQProducer 不可用，使用 StreamBridge 循环批量发送");
        return super.sendBatch(topic, messages);
    }

    private <T> List<MqSendResult> sendBatchWithProducer(String topic,
                                                          List<MqMessage<T>> messages,
                                                          DefaultMQProducer producer) {
        // 逐条触发 beforeSend 拦截器
        for (MqMessage<T> mqMsg : messages) {
            fireBeforeSend(topic, mqMsg.getKey(), mqMsg.getPayload());
        }

        List<org.apache.rocketmq.common.message.Message> rocketMsgs = new ArrayList<>(messages.size());
        for (MqMessage<T> mqMsg : messages) {
            byte[] body = converter.serialize(mqMsg.getPayload());
            org.apache.rocketmq.common.message.Message rocketMsg =
                    new org.apache.rocketmq.common.message.Message(topic, body);
            if (StringUtils.isNotBlank(mqMsg.getKey())) {
                rocketMsg.setKeys(mqMsg.getKey());
            }
            // 注入上下文和自定义头部到 RocketMQ properties
            Message<byte[]> springMsg = buildMessage(mqMsg.getKey(),
                    mqMsg.getPayload(), mqMsg.getHeaders());
            springMsg.getHeaders().forEach((headerKey, headerValue) ->
                    rocketMsg.getProperties().put(headerKey,
                            headerValue != null ? headerValue.toString() : ""));
            rocketMsgs.add(rocketMsg);
        }

        try {
            org.apache.rocketmq.client.producer.SendResult result = producer.send(rocketMsgs);
            log.debug("RocketMQ 批量发送成功: topic={}, count={}, msgId={}",
                    topic, rocketMsgs.size(), result.getMsgId());

            // 逐条触发 afterSend 拦截器
            List<MqSendResult> results = new ArrayList<>(rocketMsgs.size());
            long timestamp = System.currentTimeMillis();
            for (int i = 0; i < rocketMsgs.size(); i++) {
                MqSendResult sendResult = MqSendResult.builder()
                        .messageId(result.getMsgId())
                        .topic(topic)
                        .timestamp(timestamp)
                        .build();
                results.add(sendResult);
                fireAfterSend(topic, sendResult);
            }
            return results;

        } catch (Exception e) {
            log.error("RocketMQ 批量发送失败: topic={}, count={}", topic, rocketMsgs.size(), e);
            for (MqMessage<T> ignored : messages) {
                fireOnSendError(topic, e);
            }
            throw new MqException("mq.send.failed", new Object[]{topic + ", count=" + rocketMsgs.size()});
        }
    }

    /**
     * 懒获取 DefaultMQProducer
     * <p>优先复用容器中已有的 Bean（如 rocketmq-spring-boot 提供），不存在则为 null 并回退</p>
     */
    private DefaultMQProducer getBatchProducer() {
        if (!batchProducerLookedUp) {
            synchronized (this) {
                if (!batchProducerLookedUp) {
                    this.batchProducer = batchProducerProvider.getIfAvailable();
                    this.batchProducerLookedUp = true;
                    if (this.batchProducer != null) {
                        log.info("RocketMQ 批量发送复用已有 Producer: group={}",
                                this.batchProducer.getProducerGroup());
                    }
                }
            }
        }
        return this.batchProducer;
    }

    private int mapDurationToDelayLevel(Duration delay) {
        long seconds = delay.getSeconds();
        for (int i = 1; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (seconds <= DELAY_LEVEL_SECONDS[i]) {
                return i;
            }
        }
        log.warn("延迟时长 {} 超过 RocketMQ 最大延迟级别（2h），将使用级别 18", delay);
        return 18;
    }
}
