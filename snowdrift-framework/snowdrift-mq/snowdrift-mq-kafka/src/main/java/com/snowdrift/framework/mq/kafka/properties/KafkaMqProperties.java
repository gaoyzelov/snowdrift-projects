package com.snowdrift.framework.mq.kafka.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka 消息队列配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.mq.kafka")
public class KafkaMqProperties {

    /**
     * 是否启用 Kafka（需显式设为 true，缺省视为未启用，模块由 snowdrift.mq.kafka.enabled=true 激活）
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;
}
