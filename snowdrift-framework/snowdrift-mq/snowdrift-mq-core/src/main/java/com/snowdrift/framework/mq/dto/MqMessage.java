package com.snowdrift.framework.mq.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 消息信封
 * <p>
 * 统一封装消息体、路由 Key 和自定义头部，用于批量发送等场景。
 * </p>
 *
 * @param <T> 消息体类型
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Builder
public class MqMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息体
     */
    private T payload;

    /**
     * 消息 Key
     * — Kafka: partitioning key
     * — RocketMQ: sharding keys
     * — RabbitMQ: routing key
     */
    private String key;

    /**
     * 自定义消息头
     */
    private Map<String, String> headers;

    /**
     * 消息时间戳（毫秒）
     * @deprecated 此字段当前未被框架使用，保留仅为序列化兼容。将在后续大版本中移除。
     */
    @Deprecated
    private Long timestamp;
}
