package com.snowdrift.framework.mq.rocketmq.config;

import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.core.RocketMqTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift RocketMQ MQ 自动配置
 * <p>
 * 当 {@code snowdrift.mq.rocketmq.enabled=true} 且 RocketMQ Binder 在 classpath 中时激活。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RocketMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.rocketmq", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "com.alibaba.cloud.stream.binder.rocketmq.config.RocketMQBinderConfiguration")
public class SnowdriftRocketMqConfiguration {

    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public RocketMqTemplate rocketMqTemplate(StreamBridge streamBridge, MqProperties mqProperties) {
        log.info("Snowdrift RocketMQ MQ 模板已注册");
        return new RocketMqTemplate(streamBridge, mqProperties);
    }
}
