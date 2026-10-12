package com.snowdrift.framework.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
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
public class SnowdriftAsyncConfiguration {

    private final Executor executor;

    public SnowdriftAsyncConfiguration(@Qualifier("snowdriftAsyncExecutor") Executor executor) {
        this.executor = executor;
    }

    /**
     * 注册框架的异步配置器
     *
     * <p><b>必须用独立的 {@code @Bean} + {@code @ConditionalOnMissingBean}，而不是让本配置类
     * {@code implements AsyncConfigurer}。</b>Spring 只允许容器中存在一个 {@link AsyncConfigurer}
     * （{@code AbstractAsyncConfiguration#setConfigurers} 多于一个即抛
     * {@code IllegalStateException("Only one AsyncConfigurer may exist")}），
     * 而宿主自定义 {@code AsyncConfigurer}（设置自己的异步线程池的常规做法）在“配置类自身实现”的写法下
     * 必然与之冲突；更糟的是该异常包在惰性 {@code SingletonSupplier} 里，
     * 直到<b>首次 {@code @Async} 调用</b>才会抛出，远离配置现场。</p>
     *
     * @return 复用 {@code snowdriftAsyncExecutor} 的异步配置器；宿主已提供时退避
     */
    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public AsyncConfigurer snowdriftAsyncConfigurer() {
        return new AsyncConfigurer() {

            @Override
            public Executor getAsyncExecutor() {
                return executor;
            }

            /**
             * 异步执行异常处理
             *
             * @return AsyncUncaughtExceptionHandler
             */
            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return (ex, method, params) -> log.error("异步执行方法: {}, 参数: {}, 发生异常",
                        method.getName(),
                        Arrays.toString(params),
                        ex);
            }
        };
    }
}
