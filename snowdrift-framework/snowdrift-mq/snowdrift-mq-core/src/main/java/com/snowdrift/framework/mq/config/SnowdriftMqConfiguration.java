package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.mq.core.*;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Snowdrift MQ 通用自动配置
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowdriftMqConfiguration {

    private static final String SCS_DYNAMIC_DEST_CACHE = "spring.cloud.stream.dynamic-destination-cache-size";

    @Bean
    @ConditionalOnMissingBean(name = "mqAsyncExecutor")
    public Executor mqAsyncExecutor(MqProperties properties) {
        MqProperties.ExecutorProperties exec = properties.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(exec.getCoreSize());
        executor.setMaxPoolSize(exec.getMaxSize());
        executor.setQueueCapacity(exec.getQueueCapacity());
        executor.setKeepAliveSeconds(exec.getKeepAliveSeconds());
        executor.setThreadNamePrefix(exec.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(exec.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(exec.getAwaitTerminationSeconds());
        executor.initialize();
        log.info("MQ 异步发送线程池已初始化: core={}, max={}, queue={}",
                exec.getCoreSize(), exec.getMaxSize(), exec.getQueueCapacity());
        return executor;
    }

    /**
     * 消息转换器（默认 FastJson2）
     */
    @Bean
    @ConditionalOnMissingBean(MqMessageConverter.class)
    public MqMessageConverter mqMessageConverter() {
        return new FastJson2MqMessageConverter();
    }


    /**
     * 拦截器注册表 — 启动时自动收集所有 MqSendInterceptor Bean 并按优先级排序
     */
    @Bean
    @ConditionalOnMissingBean(MqInterceptorRegistry.class)
    public MqInterceptorRegistry mqInterceptorRegistry(List<MqSendInterceptor> interceptors) {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        interceptors.forEach(registry::register);
        log.info("MQ 拦截器注册表已初始化，注册数量: {}", interceptors.size());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean(IMqService.class)
    public DefaultMqServiceImpl mqTemplate(StreamBridge streamBridge, MqProperties properties,
                                           Executor mqAsyncExecutor, MqMessageConverter converter,
                                           MqInterceptorRegistry interceptorRegistry,
                                           MqContextPropagator contextPropagator,
                                           ConfigurableEnvironment env) {
        mapCoreProperties(properties, env);
        log.info("Snowdrift MQ 默认模板已注册（StreamBridge），拦截器数量: {}",
                interceptorRegistry.getInterceptors().size());
        return new DefaultMqServiceImpl(streamBridge, properties, mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
    }

    @Bean
    public MqListenerBeanDefinitionRegistrar mqListenerBeanDefinitionRegistrar() {
        return new MqListenerBeanDefinitionRegistrar();
    }

    @Bean
    public MqContextPropagator mqContextPropagator(MqProperties properties) {
        return new MqContextPropagator(properties);
    }

    private void mapCoreProperties(MqProperties props, ConfigurableEnvironment env) {
        if (env.getProperty(SCS_DYNAMIC_DEST_CACHE) == null) {
            env.getPropertySources().addFirst(new MapPropertySource("snowdrift-mq-core",
                    Map.of(SCS_DYNAMIC_DEST_CACHE,
                            String.valueOf(props.getDynamicDestinationCacheSize()))));
        }
    }
}
