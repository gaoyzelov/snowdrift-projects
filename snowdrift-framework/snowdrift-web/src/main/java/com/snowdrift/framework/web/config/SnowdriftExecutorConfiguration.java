package com.snowdrift.framework.web.config;

import com.alibaba.ttl.TtlRunnable;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.framework.web.properties.AsyncProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * SnowdriftExecutorConfiguration
 *
 * @author gaoyzelov
 * @date 2024/8/19 11:24
 * @description 异步线程池配置类
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
        AssertUtil.predicate(() -> properties.getCorePoolSize() <= properties.getMaxPoolSize(),
                "corePoolSize 不能大于 maxPoolSize");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize()); // 设置核心线程数
        executor.setMaxPoolSize(properties.getMaxPoolSize()); // 设置最大线程数
        executor.setQueueCapacity(properties.getQueueCapacity()); // 设置队列容量
        executor.setThreadNamePrefix(properties.getThreadNamePrefix()); // 设置线程名前缀
        executor.setTaskDecorator(SnowdriftExecutorConfiguration::propagateContext); // 设置线程上下文（TTL + MDC）
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

    /**
     * 异步任务的上下文装饰器：TTL + MDC 两套机制都要处理
     * <p>
     * {@link TtlRunnable} 只覆盖 {@code TransmittableThreadLocal}（{@code HttpContext} /
     * {@code SecurityContext}），而 traceId 存在 slf4j {@code MDC} —— 那是普通 {@code ThreadLocal}，
     * 只挂 TTL 会让异步链路的日志整体丢掉 traceId。
     * </p>
     * <p>
     * 因此：在<b>调用线程</b>快照 MDC，在 worker 上还原后再执行；任务结束后把 worker 原有的 MDC
     * 还原回去，避免池化线程把上一个请求的 traceId 带给下一个任务。
     * </p>
     *
     * @param task 原始任务
     * @return 同时携带 TTL 与 MDC 的任务
     */
    static Runnable propagateContext(Runnable task) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        Runnable ttlTask = TtlRunnable.get(task);
        Runnable delegate = ttlTask != null ? ttlTask : task;
        return () -> {
            Map<String, String> workerMdc = MDC.getCopyOfContextMap();
            applyMdc(callerMdc);
            try {
                delegate.run();
            } finally {
                applyMdc(workerMdc);
            }
        };
    }

    private static void applyMdc(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}