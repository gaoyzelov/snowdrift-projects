package com.snowdrift.framework.mq.rocketmq.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 消息队列配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.mq.rocketmq")
public class RocketMqProperties {

    /**
     * 是否启用 RocketMQ
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * NameServer 地址（如 127.0.0.1:9876）
     */
    @NotBlank
    private String nameServer = "localhost:9876";

    /**
     * 生产者组名
     */
    private String producerGroup = "snowdrift-producer";

    /**
     * 消费者组名
     */
    private String consumerGroup = "snowdrift-consumer";
}
