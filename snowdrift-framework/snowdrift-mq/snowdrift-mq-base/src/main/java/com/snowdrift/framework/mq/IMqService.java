package com.snowdrift.framework.mq;

import com.snowdrift.framework.mq.model.MqMessage;
import com.snowdrift.framework.mq.model.MqSendResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一消息发送模板接口
 * <p>
 * 屏蔽 Kafka / RocketMQ / RabbitMQ 差异，提供同步 / 异步 / 延迟 / 批量发送能力。
 * 由各实现模块基于 broker 原生客户端（如 spring-kafka / spring-amqp / rocketmq-spring）实现。
 * 各能力均以「最全参数」版本为唯一抽象（如 {@link #send(String, String, Object, Map)}）；
 * 少参数版本为接口 {@code default} 便捷委托，实现类无需重复。
 * {@link MqSendResult#getMessageId()} 与 {@link MqSendResult#getPartitionOrQueue()} 在原生路径下可返回真实 broker 元数据。
 * 消费端不使用统一注解——请在各实现模块下直接使用 broker 原生监听注解（如 {@code @KafkaListener}）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
public interface IMqService {

    // ========== 同步发送 ==========

    /**
     * 同步发送消息到指定 topic
     *
     * @param topic   目标 topic / destination
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果
     */
    default <T> MqSendResult send(String topic, T payload) {
        return send(topic, null, payload, null);
    }

    /**
     * 同步发送带 Key 的消息（用于分区 / 分片路由）
     *
     * @param topic   目标 topic
     * @param key     消息 Key
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果
     */
    default <T> MqSendResult send(String topic, String key, T payload) {
        return send(topic, key, payload, null);
    }

    /**
     * 同步发送带自定义头部的消息（各实现模块的核心抽象方法）
     *
     * @param topic   目标 topic
     * @param key     消息 Key（可空）
     * @param payload 消息体
     * @param headers 自定义消息头（可空）
     * @param <T>     消息体类型
     * @return 发送结果
     */
    <T> MqSendResult send(String topic, String key, T payload, Map<String, String> headers);

    // ========== 异步发送 ==========

    /**
     * 异步发送消息
     *
     * @param topic   目标 topic
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果 Future
     */
    default <T> CompletableFuture<MqSendResult> sendAsync(String topic, T payload) {
        return sendAsync(topic, null, payload, null);
    }

    /**
     * 异步发送带 Key 的消息
     *
     * @param topic   目标 topic
     * @param key     消息 Key
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果 Future
     */
    default <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload) {
        return sendAsync(topic, key, payload, null);
    }

    /**
     * 异步发送带自定义头部的消息（各实现模块的核心抽象方法）
     *
     * @param topic   目标 topic
     * @param key     消息 Key（可空）
     * @param payload 消息体
     * @param headers 自定义消息头（可空）
     * @param <T>     消息体类型
     * @return 发送结果 Future
     */
    <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload, Map<String, String> headers);

    // ========== 延迟发送 ==========

    /**
     * 延迟发送消息
     * <p>
     * 各 MQ 实现能力不同，遵循「原生优先、无原生则显式拒绝」：
     * — RocketMQ：原生延迟级别（1-18），由实现模块换算延迟级别；
     * — RabbitMQ：依赖 rabbitmq-delayed-message-exchange 插件（x-delay 头），未启用插件时抛 {@link UnsupportedOperationException}；
     * — Kafka：无原生延迟能力，抛 {@link UnsupportedOperationException}。
     * </p>
     *
     * @param topic   目标 topic
     * @param payload 消息体
     * @param delay   延迟时长
     * @param <T>     消息体类型
     * @return 发送结果
     */
    default <T> MqSendResult sendDelay(String topic, T payload, Duration delay) {
        return sendDelay(topic, null, payload, delay, null);
    }

    /**
     * 延迟发送带 Key 的消息
     *
     * @param topic   目标 topic
     * @param key     消息 Key
     * @param payload 消息体
     * @param delay   延迟时长
     * @param <T>     消息体类型
     * @return 发送结果
     */
    default <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay) {
        return sendDelay(topic, key, payload, delay, null);
    }

    /**
     * 延迟发送带自定义头部的消息
     * <p>
     * 默认不支持延迟发送并抛 {@link UnsupportedOperationException}；
     * 具备原生延迟能力的实现（如 RocketMQ 延迟级别、RabbitMQ x-delay 插件）应覆写本方法。
     * </p>
     *
     * @param topic   目标 topic
     * @param key     消息 Key（可空）
     * @param payload 消息体
     * @param delay   延迟时长
     * @param headers 自定义消息头（可空）
     * @param <T>     消息体类型
     * @return 发送结果
     */
    default <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        throw new UnsupportedOperationException("当前消息中间件不支持延迟消息发送");
    }

    // ========== 批量发送 ==========

    /**
     * 批量发送消息到同一 topic
     * <p>
     * 默认实现逐条发送，非原子操作——部分成功部分失败时抛出异常，已发送的消息不回滚。
     * Kafka 场景下建议使用原生 producer batch 获得更好的吞吐量。
     * </p>
     *
     * @param topic    目标 topic
     * @param messages 消息列表
     * @param <T>      消息体类型
     * @return 发送结果列表（顺序与输入一致）
     */
    <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages);
}
