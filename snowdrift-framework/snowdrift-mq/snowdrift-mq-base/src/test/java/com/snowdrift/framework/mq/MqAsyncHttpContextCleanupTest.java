package com.snowdrift.framework.mq;

import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MqAsyncHttpContextCleanupTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description CTX-01：mqAsyncExecutor 是裸线程池（未包装 TtlRunnable），而 HttpContextHolder 是
 *              TransmittableThreadLocal（继承自 InheritableThreadLocal）——worker 若在某个请求
 *              处理期间首次创建会永久继承该请求的 HttpContext，且异步路径只清 Security 与 MDC。
 *              本测试钉住「异步任务内与任务之后都不得读到该残留上下文」
 * @since 1.0.0
 */
class MqAsyncHttpContextCleanupTest {

    /** 在原生发送阶段（即任务体内）记录读到的 HttpContext */
    static class ProbeMqService extends AbstractMqService {

        final List<HttpContext> seenInTask = new CopyOnWriteArrayList<>();

        ProbeMqService(Executor executor) {
            super(executor, new FastJson2MqMessageConverter(), new MqInterceptorRegistry(), propagator());
        }

        @Override
        protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
            seenInTask.add(HttpContextHolder.getContext());
            return MqSendResult.builder().topic(topic).messageId("id").timestamp(1L).build();
        }
    }

    private static MqContextPropagator propagator() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        return new MqContextPropagator(properties);
    }

    @AfterEach
    void clear() {
        HttpContextHolder.clear();
    }

    @Test
    @DisplayName("worker 在 HttpContext 已设置期间创建：异步任务体内不得读到该残留上下文")
    void inheritedHttpContextIsNotVisibleInsideTask() throws Exception {
        // 刻意不预热线程池：让 worker 在 setContext 之后才创建，从而走 InheritableThreadLocal 继承路径
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            HttpContextHolder.setContext(HttpContext.builder().ip("10.0.0.1").uri("/stale-request").build());
            ProbeMqService service = new ProbeMqService(pool);

            service.sendAsync("topic", "key", "payload").get(10, TimeUnit.SECONDS);

            assertThat(service.seenInTask).containsExactly((HttpContext) null);
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("任务结束后 worker 上不残留 HttpContext：下一个任务同样读不到")
    void httpContextDoesNotRemainOnWorkerBetweenTasks() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            HttpContextHolder.setContext(HttpContext.builder().ip("10.0.0.2").uri("/first").build());
            ProbeMqService service = new ProbeMqService(pool);

            service.sendAsync("topic", "key", "first").get(10, TimeUnit.SECONDS);
            // 调用方上下文已清（模拟请求结束），再发一次
            HttpContextHolder.clear();
            service.sendAsync("topic", "key", "second").get(10, TimeUnit.SECONDS);

            assertThat(service.seenInTask).hasSize(2).containsOnlyNulls();
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
