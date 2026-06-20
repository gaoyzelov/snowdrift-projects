package com.snowdrift.framework.mq.kafka.config;

import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.kafka.core.KafkaMqTemplate;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift Kafka MQ 自动配置
 * <p>
 * 当 {@code snowdrift.mq.kafka.enabled=true} 且 Kafka Binder 在 classpath 中时激活。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(KafkaMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.kafka", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration")
public class SnowdriftKafkaMqConfiguration {

    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public KafkaMqTemplate kafkaMqTemplate(StreamBridge streamBridge, MqProperties mqProperties) {
        log.info("Snowdrift Kafka MQ 模板已注册");
        return new KafkaMqTemplate(streamBridge, mqProperties);
    }
}
