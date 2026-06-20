package com.snowdrift.framework.mq.rabbitmq.config;

import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.core.RabbitMqTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift RabbitMQ MQ 自动配置
 * <p>
 * 当 {@code snowdrift.mq.rabbitmq.enabled=true} 且 RabbitMQ Binder 在 classpath 中时激活。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.rabbitmq", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.cloud.stream.binder.rabbit.config.RabbitMessageChannelBinderConfiguration")
public class SnowdriftRabbitMqConfiguration {

    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public RabbitMqTemplate rabbitMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                                              RabbitMqProperties rabbitProperties) {
        log.info("Snowdrift RabbitMQ MQ 模板已注册");
        return new RabbitMqTemplate(streamBridge, mqProperties, rabbitProperties);
    }
}
