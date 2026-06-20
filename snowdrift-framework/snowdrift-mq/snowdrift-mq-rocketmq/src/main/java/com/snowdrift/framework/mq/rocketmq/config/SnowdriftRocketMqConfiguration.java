package com.snowdrift.framework.mq.rocketmq.config;

import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.core.MqSendInterceptor;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.core.RocketMqTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.ObjectProvider;
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
 * Snowdrift RocketMQ MQ 自动配置
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

    private static final String SCS_ROCKETMQ_BINDER_PREFIX = "spring.cloud.stream.rocketmq.binder.";

    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public RocketMqTemplate rocketMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                                              Executor mqAsyncExecutor, MqMessageConverter converter,
                                              RocketMqProperties rocketProperties,
                                              ObjectProvider<DefaultMQProducer> batchProducerProvider,
                                              ConfigurableEnvironment env,
                                              List<MqSendInterceptor> interceptors) {
        mapRocketMqProperties(rocketProperties, env);
        log.info("Snowdrift RocketMQ MQ 模板已注册，拦截器数量: {}", interceptors.size());
        return new RocketMqTemplate(streamBridge, mqProperties, mqAsyncExecutor, converter,
                batchProducerProvider, rocketProperties, interceptors);
    }

    private void mapRocketMqProperties(RocketMqProperties props, ConfigurableEnvironment env) {
        Map<String, Object> mapped = new HashMap<>();
        if (StringUtils.isNotBlank(props.getNameServer())) {
            setIfAbsent(mapped, env, SCS_ROCKETMQ_BINDER_PREFIX + "name-server",
                    props.getNameServer());
        }
        if (StringUtils.isNotBlank(props.getProducerGroup())) {
            setIfAbsent(mapped, env, SCS_ROCKETMQ_BINDER_PREFIX + "producer.group",
                    props.getProducerGroup());
        }
        if (StringUtils.isNotBlank(props.getConsumerGroup())) {
            setIfAbsent(mapped, env, SCS_ROCKETMQ_BINDER_PREFIX + "consumer.group",
                    props.getConsumerGroup());
        }
        if (!mapped.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource("snowdrift-mq-rocketmq", mapped));
        }
    }

    private static void setIfAbsent(Map<String, Object> map, ConfigurableEnvironment env,
                                     String key, String value) {
        if (env.getProperty(key) == null) {
            map.put(key, value);
        }
    }
}
