package com.snowdrift.framework.web.config;

import com.snowdrift.framework.log.util.LogTraceUtil;
import com.snowdrift.framework.web.properties.AsyncProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftAsyncTraceIdPropagationTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description traceId 存在 slf4j MDC（普通 ThreadLocal）而非 TransmittableThreadLocal，
 *              只挂 TtlRunnable 不会把它带进异步线程 —— 异步链路的日志会丢 traceId
 * @since 1.0.0
 */
class SnowdriftAsyncTraceIdPropagationTest {

    private final Executor executor = new SnowdriftExecutorConfiguration(new AsyncProperties()).getAsyncExecutor();

    @AfterEach
    void clearTraceId() {
        LogTraceUtil.clearTraceId();
    }

    @Test
    @DisplayName("异步线程内可读到调用方的 traceId")
    void traceIdPropagatesIntoAsyncThread() throws Exception {
        LogTraceUtil.setTraceId("trace-from-request");

        String seenInWorker = CompletableFuture
                .supplyAsync(LogTraceUtil::getTraceId, executor)
                .get(10, TimeUnit.SECONDS);

        assertThat(seenInWorker).isEqualTo("trace-from-request");
    }

    @Test
    @DisplayName("异步任务结束后 worker 上不残留 traceId（池化线程不得把上一个请求的 traceId 带给下一个）")
    void traceIdDoesNotLeakBetweenTasks() throws Exception {
        LogTraceUtil.setTraceId("trace-first");
        CompletableFuture.runAsync(() -> { }, executor).get(10, TimeUnit.SECONDS);
        LogTraceUtil.clearTraceId();

        String seenInWorker = CompletableFuture
                .supplyAsync(LogTraceUtil::getTraceId, executor)
                .get(10, TimeUnit.SECONDS);

        assertThat(seenInWorker).isNull();
    }
}
