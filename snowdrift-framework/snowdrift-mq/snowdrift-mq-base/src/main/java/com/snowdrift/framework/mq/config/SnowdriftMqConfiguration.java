package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.interceptor.MqSendInterceptor;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
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
    public MqContextPropagator mqContextPropagator(MqProperties properties) {
        return new MqContextPropagator(properties);
    }

    /**
     * 多 binder 同时启用守卫：同一时间只允许一种 {@code snowdrift.mq.<x>.enabled=true}
     */
    @Bean
    public MqBinderActivationGuard mqBinderActivationGuard() {
        return new MqBinderActivationGuard();
    }
}
