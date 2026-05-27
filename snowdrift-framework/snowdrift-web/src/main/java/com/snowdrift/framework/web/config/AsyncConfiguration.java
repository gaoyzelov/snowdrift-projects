package com.snowdrift.framework.web.config;

import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.web.properties.AsyncProperties;
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

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncConfiguration
 *
 * @author 83674
 * @date 2024/8/19 11:24
 * @description 异步配置类
 * @since 1.0.0
 */
@Slf4j
@EnableAsync
@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.async", name = "enabled", havingValue = "true")
public class AsyncConfiguration implements AsyncConfigurer {

    @Resource
    private AsyncProperties asyncProperties;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize()); // 设置核心线程数
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize()); // 设置最大线程数
        executor.setQueueCapacity(asyncProperties.getQueueCapacity()); // 设置队列容量
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix()); // 设置线程名前缀
        executor.setTaskDecorator(taskDecorator()); // 设置线程上下文
        executor.setWaitForTasksToCompleteOnShutdown(asyncProperties.getWaitForTasksToCompleteOnShutdown()); // 设置优雅关闭
        executor.setAwaitTerminationSeconds(asyncProperties.getAwaitTerminationSeconds()); // 设置等待时间
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 设置拒绝策略
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("异步执行方法: {}, 参数: {}, 发生异常",
                    method.getName(),
                    Arrays.toString(params),
                    ex);
        };
    }

    public TaskDecorator taskDecorator() {
        return runnable -> {
            final HttpContext httpContext = HttpContextHolder.getContext();
            final SecurityContext securityContext = SecurityContextHolder.getContext();
            return () -> {
                try {
                    HttpContextHolder.setContext(httpContext);
                    SecurityContextHolder.setContext(securityContext);
                    runnable.run();
                } finally {
                    // 清理上下文，避免内存泄漏
                    HttpContextHolder.clear();
                    SecurityContextHolder.clear();
                }
            };
        };
    }
}