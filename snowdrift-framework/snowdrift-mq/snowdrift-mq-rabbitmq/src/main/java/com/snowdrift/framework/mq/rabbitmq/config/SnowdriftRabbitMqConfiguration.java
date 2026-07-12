package com.snowdrift.framework.mq.rabbitmq.config;

import com.snowdrift.framework.mq.core.IMqService;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.core.MqInterceptorRegistry;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.service.RabbitMqServiceImpl;
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
 * Snowdrift RabbitMQ MQ 自动配置
 * <p>
 * 将 {@code snowdrift.mq.rabbitmq.*} 属性自动映射为 Spring Cloud Stream Rabbit Binder 属性。
 * 当 {@code snowdrift.mq.rabbitmq.enabled=true} 且 RabbitMQ Binder 在 classpath 中时激活。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.rabbitmq", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.cloud.stream.binder.rabbit.config.RabbitMessageChannelBinderConfiguration")
public class SnowdriftRabbitMqConfiguration {

    private static final String SCS_RABBIT_BINDER_PREFIX = "spring.cloud.stream.rabbit.binder.";

    @Bean
    @ConditionalOnMissingBean(IMqService.class)
    public RabbitMqServiceImpl rabbitMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                                                RabbitMqProperties rabbitProperties,
                                                Executor mqAsyncExecutor, MqMessageConverter converter,
                                                ConfigurableEnvironment env,
                                                MqInterceptorRegistry interceptorRegistry,
                                                MqContextPropagator contextPropagator) {
        mapRabbitMqProperties(rabbitProperties, env);
        log.info("Snowdrift RabbitMQ MQ 模板已注册，拦截器数量: {}", interceptorRegistry.getInterceptors().size());
        return new RabbitMqServiceImpl(streamBridge, mqProperties, rabbitProperties, mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
    }

    /**
     * 将 snowdrift.mq.rabbitmq.* 映射为 spring.cloud.stream.rabbit.binder.*
     */
    private void mapRabbitMqProperties(RabbitMqProperties props, ConfigurableEnvironment env) {
        Map<String, Object> mapped = new HashMap<>();
        if (StringUtils.isNotBlank(props.getAddresses())) {
            setIfAbsent(mapped, env, SCS_RABBIT_BINDER_PREFIX + "addresses",
                    props.getAddresses());
        }
        if (StringUtils.isNotBlank(props.getVirtualHost())) {
            setIfAbsent(mapped, env, SCS_RABBIT_BINDER_PREFIX + "virtual-host",
                    props.getVirtualHost());
        }
        if (StringUtils.isNotBlank(props.getUsername())) {
            setIfAbsent(mapped, env, SCS_RABBIT_BINDER_PREFIX + "username",
                    props.getUsername());
        }
        if (StringUtils.isNotBlank(props.getPassword())) {
            setIfAbsent(mapped, env, SCS_RABBIT_BINDER_PREFIX + "password",
                    props.getPassword());
        }
        if (!mapped.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource("snowdrift-mq-rabbitmq", mapped));
        }
    }

    private static void setIfAbsent(Map<String, Object> map, ConfigurableEnvironment env,
                                     String key, String value) {
        if (env.getProperty(key) == null) {
            map.put(key, value);
        }
    }
}
