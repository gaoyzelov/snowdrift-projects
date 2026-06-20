package com.snowdrift.framework.mq.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息发送结果
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
     */
    private String messageId;

    /**
     * 发送到的 topic / destination
     */
    private String topic;

    /**
     * 分区号或队列名
     * — Kafka: partition
     * — RocketMQ: MessageQueue
     * — RabbitMQ: queue name
     */
    private String partitionOrQueue;

    /**
     * 发送时间戳（毫秒）
     */
    private Long timestamp;
}
