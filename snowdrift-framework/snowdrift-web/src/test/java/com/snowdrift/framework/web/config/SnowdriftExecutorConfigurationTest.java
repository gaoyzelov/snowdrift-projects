package com.snowdrift.framework.web.config;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.web.properties.AsyncProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowdriftExecutorConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 异步线程池配置黑盒测试：属性映射、corePoolSize>maxPoolSize 启动校验、拒绝策略抛 BizException(TOO_MANY_REQUESTS)
 * @since 1.0.0
 */
class SnowdriftExecutorConfigurationTest {

    private AsyncProperties props(int core, int max) {
        AsyncProperties properties = new AsyncProperties();
        properties.setCorePoolSize(core);
        properties.setMaxPoolSize(max);
        properties.setQueueCapacity(256);
        properties.setThreadNamePrefix("async-");
        properties.setWaitForTasksToCompleteOnShutdown(true);
        properties.setAwaitTerminationSeconds(60);
        return properties;
    }

    @Test
    @DisplayName("按属性构建线程池，池参数正确映射")
    void buildsExecutorFromProperties() {
        Executor executor = new SnowdriftExecutorConfiguration(props(2, 10)).getAsyncExecutor();
        try {
            assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
            assertThat(pool.getCorePoolSize()).isEqualTo(2);
            assertThat(pool.getMaxPoolSize()).isEqualTo(10);
            assertThat(pool.getThreadNamePrefix()).isEqualTo("async-");
        } finally {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }

    @Test
    @DisplayName("corePoolSize > maxPoolSize 时启动即抛 BizException")
    void rejectsInvalidPoolSizes() {
        SnowdriftExecutorConfiguration configuration = new SnowdriftExecutorConfiguration(props(20, 10));

        assertThatThrownBy(configuration::getAsyncExecutor)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("corePoolSize 不能大于 maxPoolSize");
    }

    @Test
    @DisplayName("拒绝策略：线程池饱和时抛 BizException(TOO_MANY_REQUESTS)")
    void rejectionHandlerThrowsTooManyRequests() {
        Executor executor = new SnowdriftExecutorConfiguration(props(1, 1)).getAsyncExecutor();
        try {
            ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
            RejectedExecutionHandler rejectionHandler = pool.getThreadPoolExecutor().getRejectedExecutionHandler();

            assertThatThrownBy(() -> rejectionHandler.rejectedExecution(() -> {
            }, pool.getThreadPoolExecutor()))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getCode())
                    .isEqualTo(ResultCode.TOO_MANY_REQUESTS.code());
        } finally {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }
}
