package com.snowdrift.framework.mq.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息发送结果
 * <p>
 * 由各实现模块基于 broker 原生客户端填充；在原生发送路径下 {@link #messageId} 与
 * {@link #partitionOrQueue} 可拿到真实 broker 元数据（如 Kafka 的 topic-partition-offset）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Builder
public class MqSendResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID（broker 返回的消息唯一标识）
     * <p>原生路径：Kafka = {@code topic-partition-offset}；其它 broker 按其原生消息 ID 约定。</p>
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
     */
    private String partitionOrQueue;

    /**
     * 发送时间戳（毫秒）
     */
    private Long timestamp;
}
