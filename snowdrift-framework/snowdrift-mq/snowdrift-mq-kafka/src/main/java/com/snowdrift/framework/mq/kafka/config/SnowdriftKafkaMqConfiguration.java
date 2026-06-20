package com.snowdrift.framework.mq.kafka.config;

import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.core.MqSendInterceptor;
import com.snowdrift.framework.mq.kafka.core.KafkaMqTemplate;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Snowdrift Kafka MQ 自动配置
 * <p>
 * 将 {@code snowdrift.mq.kafka.*} 属性自动映射为 Spring Cloud Stream Kafka Binder 属性。
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

    /**
     * SCS Kafka binder 属性前缀
     */
    private static final String SCS_KAFKA_BINDER_PREFIX = "spring.cloud.stream.kafka.binder.";

    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public KafkaMqTemplate kafkaMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                                            Executor mqAsyncExecutor, MqMessageConverter converter,
                                            KafkaMqProperties kafkaProperties, ConfigurableEnvironment env,
                                            List<MqSendInterceptor> interceptors) {
        mapKafkaProperties(kafkaProperties, env);
        log.info("Snowdrift Kafka MQ 模板已注册，拦截器数量: {}", interceptors.size());
        return new KafkaMqTemplate(streamBridge, mqProperties, mqAsyncExecutor, converter, interceptors);
    }

    /**
     * 将 snowdrift.mq.kafka.* 映射为 spring.cloud.stream.kafka.binder.*
     * <p>仅当 SCS 侧未显式配置时才设置，避免覆盖用户自定义的 SCS 配置</p>
     */
    private void mapKafkaProperties(KafkaMqProperties props, ConfigurableEnvironment env) {
        Map<String, Object> mapped = new HashMap<>();
        if (StringUtils.isNotBlank(props.getBootstrapServers())) {
            setIfAbsent(mapped, env, SCS_KAFKA_BINDER_PREFIX + "brokers",
                    props.getBootstrapServers());
        }
        if (StringUtils.isNotBlank(props.getAcks())) {
            setIfAbsent(mapped, env, SCS_KAFKA_BINDER_PREFIX + "required-acks",
                    props.getAcks());
        }
        if (StringUtils.isNotBlank(props.getCompressionType())) {
            setIfAbsent(mapped, env, SCS_KAFKA_BINDER_PREFIX + "configuration.compression.type",
                    props.getCompressionType());
        }
        if (!mapped.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource("snowdrift-mq-kafka", mapped));
        }
    }

    private static void setIfAbsent(Map<String, Object> map, ConfigurableEnvironment env,
                                     String key, String value) {
        if (env.getProperty(key) == null) {
            map.put(key, value);
        }
    }
}
