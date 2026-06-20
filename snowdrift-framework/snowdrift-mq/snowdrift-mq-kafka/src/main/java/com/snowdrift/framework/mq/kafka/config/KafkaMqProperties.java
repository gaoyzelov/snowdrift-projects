package com.snowdrift.framework.mq.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka 消息队列配置属性
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.mq.kafka")
public class KafkaMqProperties {

    /**
     * 是否启用 Kafka
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Kafka Broker 地址列表（逗号分隔）
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * 生产者确认级别: 0, 1, all
     */
    private String acks = "1";

    /**
     * 压缩类型: none, gzip, snappy, lz4, zstd
     */
    private String compressionType = "none";
}
