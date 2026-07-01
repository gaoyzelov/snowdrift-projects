package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一消息发送模板接口
 * <p>
 * 屏蔽 Kafka / RocketMQ / RabbitMQ 差异，提供同步 / 异步 / 延迟发送能力。
 * 内部基于 Spring Cloud Stream 的 {@code StreamBridge} 实现，
 * 用户仍可绕过本接口直接使用 StreamBridge 或 @Bean Consumer。
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
    <T> MqSendResult send(String topic, T payload);

    /**
     * 同步发送带 Key 的消息（用于分区 / 分片路由）
     *
     * @param topic   目标 topic
     * @param key     消息 Key
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果
     */
    <T> MqSendResult send(String topic, String key, T payload);

    /**
     * 同步发送带自定义头部的消息
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
    <T> CompletableFuture<MqSendResult> sendAsync(String topic, T payload);

    /**
     * 异步发送带 Key 的消息
     *
     * @param topic   目标 topic
     * @param key     消息 Key
     * @param payload 消息体
     * @param <T>     消息体类型
     * @return 发送结果 Future
     */
    <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload);

    /**
     * 异步发送带自定义头部的消息
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
     * 各 MQ 实现不同：
     * — RocketMQ：原生延迟级别（1-18）
     * — RabbitMQ：x-delay 插件 或 x-message-ttl + DLX
     * — Kafka：不支持，降级为即时发送并输出 WARN 日志
     * </p>
     *
     * @param topic   目标 topic
     * @param payload 消息体
     * @param delay   延迟时长
     * @param <T>     消息体类型
     * @return 发送结果
     */
    <T> MqSendResult sendDelay(String topic, T payload, Duration delay);

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
    <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay);

    /**
     * 延迟发送带自定义头部的消息
     *
     * @param topic   目标 topic
     * @param key     消息 Key（可空）
     * @param payload 消息体
     * @param delay   延迟时长
     * @param headers 自定义消息头（可空）
     * @param <T>     消息体类型
     * @return 发送结果
     */
    <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers);

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
