package com.snowdrift.framework.mq.rabbitmq.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RabbitMQ 消息队列配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.mq.rabbitmq")
public class RabbitMqProperties {

    /**
     * 是否启用 RabbitMQ
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * 是否启用延迟消息插件（rabbitmq-delayed-message-exchange）
     * <p>
     * 启用后使用 x-delay header 实现延迟消息；未启用时 {@code sendDelay} 抛 {@link UnsupportedOperationException}，
     * 不做可能造成消息被静默丢弃的 TTL 降级。
     * </p>
     */
    private Boolean delayPluginEnabled = false;
}
