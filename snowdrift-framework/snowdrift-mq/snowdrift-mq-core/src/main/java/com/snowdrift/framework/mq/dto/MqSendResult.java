package com.snowdrift.framework.mq.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息发送结果
 * <p>
 * 当前基于 Spring Cloud Stream 的 {@code StreamBridge.send()} 实现，
 * 该 API 的返回值为 {@code boolean}，无法获取 Broker 侧的 messageId、partition 等元数据。
 * 因此 {@link #messageId} 和 {@link #partitionOrQueue} 在当前实现中始终为 {@code null}。
 * 如需这些信息，可直接使用各 MQ 的原生 SDK 发送。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Builder
public class MqSendResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID（MQ broker 返回的消息唯一标识）
     * <p>
     * <b>注意：基于 StreamBridge 的实现中此字段始终为 null。</b>
     * StreamBridge.send() 返回 boolean，不暴露 broker 侧的 messageId。
     * </p>
     */
    private String messageId;

    /**
     * 发送到的 topic / destination
     */
    private String topic;

    /**
     * 分区号或队列名
     * <p>
     * — Kafka: partition<br>
     * — RocketMQ: MessageQueue<br>
     * — RabbitMQ: queue name
     * </p>
     * <p>
     * <b>注意：基于 StreamBridge 的实现中此字段始终为 null。</b>
     * </p>
     */
    private String partitionOrQueue;

    /**
     * 发送时间戳（毫秒）
     */
    private Long timestamp;
}
