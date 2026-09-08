package com.snowdrift.framework.web.config;

import com.alibaba.ttl.TtlRunnable;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.framework.web.properties.AsyncProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * SnowdriftAsyncConfiguration
 *
 * @author gaoyzelov
 * @date 2024/8/19 11:24
 * @description 异步配置类
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AsyncProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.async", name = "enabled", havingValue = "true")
public class SnowdriftExecutorConfiguration {

    private final AsyncProperties properties;

    public SnowdriftExecutorConfiguration(AsyncProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "snowdriftAsyncExecutor")
    public Executor getAsyncExecutor() {
        AssertUtil.isTrue(properties.getCorePoolSize() <= properties.getMaxPoolSize(),
                "corePoolSize 不能大于 maxPoolSize");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize()); // 设置核心线程数
        executor.setMaxPoolSize(properties.getMaxPoolSize()); // 设置最大线程数
        executor.setQueueCapacity(properties.getQueueCapacity()); // 设置队列容量
        executor.setThreadNamePrefix(properties.getThreadNamePrefix()); // 设置线程名前缀
        executor.setTaskDecorator(TtlRunnable::get); // 设置线程上下文
        executor.setWaitForTasksToCompleteOnShutdown(properties.getWaitForTasksToCompleteOnShutdown()); // 设置优雅关闭
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds()); // 设置等待时间
        executor.setRejectedExecutionHandler((r, e) -> {
            // 直接拒绝
            log.error("线程池已满，任务提交失败");
            throw new BizException(ResultCode.TOO_MANY_REQUESTS);
        });
        executor.initialize();
        return executor;
    }
}