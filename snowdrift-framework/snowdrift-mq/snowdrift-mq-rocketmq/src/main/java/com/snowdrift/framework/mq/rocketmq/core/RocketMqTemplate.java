package com.snowdrift.framework.mq.rocketmq.core;

import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.config.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

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
 * 批量发送使用 RocketMQ 原生 {@link DefaultMQProducer#send(Collection)}（单次网络请求）。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RocketMqTemplate extends DefaultMqTemplate {

    /**
     * RocketMQ 延迟级别 Header 名
     * <p>
     * 对应 {@code org.apache.rocketmq.common.message.MessageConst.PROPERTY_DELAY_TIME_LEVEL}，
     * 字符串值为 {@code "DELAY"}。
     * </p>
     */
    private static final String ROCKETMQ_DELAY_LEVEL_HEADER = "DELAY";

    /**
     * RocketMQ 预定义延迟级别对应的秒数
     */
    private static final long[] DELAY_LEVEL_SECONDS = {
        0, 1, 5, 10, 30, 60, 120, 180, 240, 300,
        360, 420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    private final DefaultMQProducer batchProducer;
    private final RocketMqProperties rocketProperties;

    public RocketMqTemplate(StreamBridge streamBridge, MqProperties properties,
                             Executor mqAsyncExecutor, MqMessageConverter converter,
                             DefaultMQProducer batchProducer, RocketMqProperties rocketProperties) {
        super(streamBridge, properties, mqAsyncExecutor, converter);
        this.batchProducer = batchProducer;
        this.rocketProperties = rocketProperties;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        int delayLevel = mapDurationToDelayLevel(delay);
        byte[] bytes = converter.serialize(payload);

        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes);
        builder.setHeader(ROCKETMQ_DELAY_LEVEL_HEADER, delayLevel);

        if (StringUtils.isNotBlank(key)) {
            builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
        }

        MqContextPropagator.inject(builder);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::setHeader);
        }

        Message<byte[]> message = builder.build();
        boolean success = streamBridge.send(topic, message);
        if (!success) {
            log.error("RocketMQ 延迟消息发送失败: topic={}, delayLevel={}", topic, delayLevel);
            throw new MqException("mq.send.failed", new Object[]{topic});
        }

        log.debug("RocketMQ 延迟消息发送成功: topic={}, delayLevel={}", topic, delayLevel);
        return MqSendResult.builder().topic(topic).timestamp(System.currentTimeMillis()).build();
    }

    // ========== 批量发送（RocketMQ 原生 batch） ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // 构建 RocketMQ 原生 Message 列表
        List<org.apache.rocketmq.common.message.Message> rocketMsgs = new ArrayList<>(messages.size());
        for (MqMessage<T> mqMsg : messages) {
            byte[] body = converter.serialize(mqMsg.getPayload());
            org.apache.rocketmq.common.message.Message rocketMsg =
                    new org.apache.rocketmq.common.message.Message(topic, body);
            if (StringUtils.isNotBlank(mqMsg.getKey())) {
                rocketMsg.setKeys(mqMsg.getKey());
            }
            rocketMsgs.add(rocketMsg);
        }

        try {
            // 单次网络请求批量发送（比逐条 send() 快 10x+）
            org.apache.rocketmq.client.producer.SendResult result = batchProducer.send(rocketMsgs);
            log.debug("RocketMQ 批量发送成功: topic={}, count={}, msgId={}",
                    topic, rocketMsgs.size(), result.getMsgId());

            List<MqSendResult> results = new ArrayList<>(rocketMsgs.size());
            long timestamp = System.currentTimeMillis();
            for (MqMessage<T> msg : messages) {
                results.add(MqSendResult.builder()
                        .messageId(result.getMsgId())
                        .topic(topic)
                        .timestamp(timestamp)
                        .build());
            }
            return results;

        } catch (Exception e) {
            log.error("RocketMQ 批量发送失败: topic={}, count={}", topic, rocketMsgs.size(), e);
            throw new MqException("mq.send.failed", new Object[]{topic + ", count=" + rocketMsgs.size()});
        }
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
