package com.snowdrift.framework.mq.rocketmq.core;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * RocketMQ 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream RocketMQ Binder。
 * 延迟消息使用 RocketMQ 原生延迟级别（1-18），自动映射 Duration 到最接近的级别。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RocketMqTemplate extends DefaultMqTemplate {

    /**
     * RocketMQ 预定义延迟级别对应的秒数
     * 1(1s) 2(5s) 3(10s) 4(30s) 5(1m) 6(2m) 7(3m) 8(4m) 9(5m)
     * 10(6m) 11(7m) 12(8m) 13(9m) 14(10m) 15(20m) 16(30m) 17(1h) 18(2h)
     */
    private static final long[] DELAY_LEVEL_SECONDS = {
        0, 1, 5, 10, 30, 60, 120, 180, 240, 300,
        360, 420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    public RocketMqTemplate(StreamBridge streamBridge, MqProperties properties) {
        super(streamBridge, properties);
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        int delayLevel = mapDurationToDelayLevel(delay);
        byte[] bytes = JSON.toJSONBytes(payload);
        String originalType = payload.getClass().getName();

        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes)
                .setHeader(MqContextPropagator.HEADER_ORIGINAL_TYPE, originalType);

        // RocketMQ 原生延迟级别 header（Spring Cloud Stream RocketMQ binder 会识别此 header）
        // 参考: org.springframework.cloud.stream.binder.rocketmq.RocketMQHeaderMapper.DELAY_LEVEL
        builder.setHeader("DELAY", delayLevel);

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
            log.error("RocketMQ 延迟消息发送失败: topic={}, delayLevel={}, duration={}", topic, delayLevel, delay);
            throw new MqException("mq.send.failed", new Object[]{topic});
        }

        log.debug("RocketMQ 延迟消息发送成功: topic={}, delayLevel={}, duration={}", topic, delayLevel, delay);
        return MqSendResult.builder()
                .topic(topic)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 将 Duration 映射为 RocketMQ 延迟级别（取最接近的上限）
     *
     * @param delay 时长
     * @return 延迟级别（1-18）
     */
    private int mapDurationToDelayLevel(Duration delay) {
        long seconds = delay.getSeconds();
        for (int i = 1; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (seconds <= DELAY_LEVEL_SECONDS[i]) {
                return i;
            }
        }
        // 超过最大级别（2小时），使用最高级别
        log.warn("延迟时长 {} 超过 RocketMQ 最大延迟级别（2h），将使用级别 18", delay);
        return 18;
    }
}
