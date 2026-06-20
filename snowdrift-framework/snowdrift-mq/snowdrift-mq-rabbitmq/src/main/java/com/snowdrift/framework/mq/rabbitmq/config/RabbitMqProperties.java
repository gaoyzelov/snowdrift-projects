package com.snowdrift.framework.mq.rabbitmq.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 消息队列配置属性
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.mq.rabbitmq")
public class RabbitMqProperties {

    /**
     * 是否启用 RabbitMQ
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * RabbitMQ 地址列表（逗号分隔），如 localhost:5672
     */
    @NotBlank
    private String addresses = "localhost:5672";

    /**
     * Virtual Host
     */
    private String virtualHost = "/";

    /**
     * 用户名
     */
    private String username = "guest";

    /**
     * 密码
     */
    private String password = "guest";

    /**
     * 是否启用延迟消息插件（rabbitmq-delayed-message-exchange）
     * <p>
     * 启用后使用 x-delay header 实现延迟消息，否则使用 x-message-ttl + DLX 方案
     * </p>
     */
    private Boolean delayPluginEnabled = false;
}
