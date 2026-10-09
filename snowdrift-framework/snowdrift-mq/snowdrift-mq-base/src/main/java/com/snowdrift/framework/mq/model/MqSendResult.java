package com.snowdrift.framework.mq.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息发送结果
 * <p>
 * 由各实现模块基于 broker 原生客户端填充。<b>各字段的可得性因 broker 而异</b>：Kafka 可拿到 broker
 * 返回的元数据；RabbitMQ 的同步 {@code send} 不返回任何回执，其 {@link #messageId} 由客户端生成、
 * {@link #partitionOrQueue} 为客户端实际使用的 routing key。逐字段语义见各字段说明。
 * </p>
 * <p>
 * 另需注意：本对象只表示"消息已交给客户端发送"，<b>不等于 broker 已接收</b>——
 * 例如 RabbitMQ 未开启 publisher-confirm 时，不可路由的消息同样会返回成功。
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
     * 消息唯一标识
     * <p>Kafka：broker 返回的 {@code topic-partition-offset}。<br>
     * RabbitMQ：客户端生成的 UUID，同时写入 AMQP 的 {@code message_id} 属性——按 AMQP 规范该属性由
     * <b>发布方</b>设置、broker 原样透传，故它不是 broker 回传值。</p>
     */
    private String messageId;

    /**
     * 发送到的 topic / destination
     */
    private String topic;

    /**
     * 分区号或路由信息
     * <p>
     * — Kafka: partition<br>
     * — RocketMQ: MessageQueue<br>
     * — RabbitMQ: 客户端实际使用的 routing key（真实投递队列由 exchange 绑定决定，客户端不可知）
     * </p>
     */
    private String partitionOrQueue;

    /**
     * 发送时间戳（毫秒）
     */
    private Long timestamp;
}
