package com.snowdrift.framework.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;
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
@EnableAsync
@AutoConfiguration(after = SnowdriftExecutorConfiguration.class)
@ConditionalOnProperty(prefix = "snowdrift.async", name = "enabled", havingValue = "true")
public class SnowdriftAsyncConfiguration implements AsyncConfigurer{

    private final Executor executor;

    public SnowdriftAsyncConfiguration(@Qualifier("snowdriftAsyncExecutor") Executor executor) {
        this.executor = executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return executor;
    }

    /**
     * 异步执行异常处理
     * @return AsyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("异步执行方法: {}, 参数: {}, 发生异常",
                method.getName(),
                Arrays.toString(params),
                ex);
    }
}