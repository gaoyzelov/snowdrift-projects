package com.snowdrift.web.config;

import com.snowdrift.core.context.SecurityContext;
import com.snowdrift.core.context.SecurityContextHolder;
import com.snowdrift.web.properties.AsyncProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncConfiguration
 *
 * @author gaoye
 * @date 2025/03/24 14:14:33
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
@EnableAsync
@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.async", name = "enabled", havingValue = "true")
public class AsyncAutoConfiguration implements AsyncConfigurer {

    @Resource
    private AsyncProperties asyncProperties;

    /**
     * 获取异步执行器
     *
     * @return Executor
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize()); // 设置核心线程数
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize()); // 设置最大线程数
        executor.setQueueCapacity(asyncProperties.getQueueCapacity()); // 设置队列容量
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix()); // 设置线程名前缀
        executor.setTaskDecorator(taskDecorator()); // 设置任务装饰器
        executor.setWaitForTasksToCompleteOnShutdown(asyncProperties.getWaitForTasksToCompleteOnShutdown()); // 设置优雅关闭
        executor.setAwaitTerminationSeconds(asyncProperties.getAwaitTerminationSeconds()); // 设置等待时间
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy()); // 设置拒绝策略
        executor.initialize();
        return executor;
    }

    /**
     * 设置任务装饰器
     *
     * @return TaskDecorator
     */
    public TaskDecorator taskDecorator() {
        return runnable -> {
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                try {
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
        };
    }

    /**
     * 异常处理
     *
     * @return AsyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("[Async Error] 异步执行方法{}异常", method.getName(), ex);
    }
}