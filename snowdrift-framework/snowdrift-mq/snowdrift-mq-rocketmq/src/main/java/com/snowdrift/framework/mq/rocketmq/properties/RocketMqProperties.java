package com.snowdrift.framework.mq.rocketmq.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RocketMQ 消息队列配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.mq.rocketmq")
public class RocketMqProperties {

    /**
     * 是否启用 RocketMQ
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;
}
