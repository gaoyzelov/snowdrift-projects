package com.snowdrift.framework.context.http;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HttpContextHolderTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Http 上下文 Holder 单元测试（含 TTL 跨线程传递语义）
 * @since 1.0.0
 */
class HttpContextHolderTest {

    @AfterEach
    void clearContext() {
        HttpContextHolder.clear();
    }

    /**
     * 先创建并预热线程池：保证池内线程在设置上下文之前就已存在，
     * 否则线程会因 InheritableThreadLocal 语义直接继承父线程的值，测不出 TTL 的传递能力
     */
    private static ExecutorService preExistingPool() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> null).get(10, TimeUnit.SECONDS);
        return pool;
    }

    private static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }

    private static HttpContext sample() {
        return HttpContext.builder().uri("/api/user").method("GET").ip("1.2.3.4").build();
    }

    @Test
    @DisplayName("Holder 为 final 且构造器私有（静态工具类约定，不可实例化/继承）")
    void notInstantiable() {
        assertThat(Modifier.isFinal(HttpContextHolder.class.getModifiers())).isTrue();
        assertThat(HttpContextHolder.class.getDeclaredConstructors())
                .isNotEmpty()
                .allSatisfy(ctor -> assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue());
    }

    @Test
    @DisplayName("set/get/clear 基本读写")
    void setGetClear() {
        HttpContext context = sample();

        HttpContextHolder.setContext(context);
        assertThat(HttpContextHolder.getContext()).isSameAs(context);

        HttpContextHolder.clear();
        assertThat(HttpContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("未设置时返回 null（与 SecurityContextHolder.getContext 的抛异常语义不同）")
    void unsetReturnsNull() {
        assertThat(HttpContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("设置 null 抛业务异常")
    void nullContextRejected() {
        assertThatThrownBy(() -> HttpContextHolder.setContext(null))
                .isInstanceOf(BizException.class)
                .hasMessage("HTTP上下文不能为空");
    }

    @Test
    @DisplayName("clear 可重复调用，且覆盖已有值")
    void clearIsIdempotent() {
        HttpContextHolder.setContext(sample());

        HttpContextHolder.clear();
        HttpContextHolder.clear();

        assertThat(HttpContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("后设置的值覆盖先前的值")
    void setOverwrites() {
        HttpContext first = sample();
        HttpContext second = HttpContext.builder().uri("/other").build();

        HttpContextHolder.setContext(first);
        HttpContextHolder.setContext(second);

        assertThat(HttpContextHolder.getContext()).isSameAs(second);
    }

    @Test
    @DisplayName("未包装的线程池拿不到上下文（必须用 TtlExecutors/TtlRunnable 包装）")
    void notTransmittedWithoutTtlWrapper() throws Exception {
        ExecutorService pool = preExistingPool();
        try {
            HttpContextHolder.setContext(sample());

            assertThat(pool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isNull();
        } finally {
            shutdown(pool);
        }
    }

    @Test
    @DisplayName("TTL 包装的线程池可拿到上下文，且是同一实例")
    void transmittedToTtlWrappedPool() throws Exception {
        ExecutorService pool = preExistingPool();
        ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
        try {
            HttpContext context = sample();
            HttpContextHolder.setContext(context);

            assertThat(ttlPool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isSameAs(context);
        } finally {
            shutdown(pool);
        }
    }

    @Test
    @DisplayName("clear 之后不再向子线程传递（请求结束清理有效）")
    void clearedContextIsNotTransmitted() throws Exception {
        ExecutorService pool = preExistingPool();
        ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
        try {
            HttpContextHolder.setContext(sample());
            HttpContextHolder.clear();

            assertThat(ttlPool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isNull();
        } finally {
            shutdown(pool);
        }
    }

    @Test
    @DisplayName("设置之后新建的 Thread 会直接继承（InheritableThreadLocal 语义，无需 TTL 包装）")
    void inheritedByThreadCreatedAfterSet() throws Exception {
        HttpContext context = sample();
        HttpContextHolder.setContext(context);

        AtomicReference<HttpContext> seen = new AtomicReference<>();
        Thread thread = new Thread(() -> seen.set(HttpContextHolder.getContext()), "ctx-test-thread");
        thread.start();
        thread.join(10_000);

        assertThat(seen.get()).isSameAs(context);
    }

    @Test
    @DisplayName("隐患：worker 在「上下文已设置期间」首次创建会被永久继承，之后 clear 也清不掉")
    void workerCreatedWhileContextSetPermanentlyInherits() throws Exception {
        // 与 preExistingPool() 相反的场景：不预热，让 worker 在 setContext 之后才被创建。
        // 此时 ThreadPoolExecutor.addWorker 在提交线程上 new Thread，worker 直接继承当前上下文，
        // 且未包装 TTL 的池不会逐任务捕获/还原 —— 这正是 MQ 异步发送线程、调度线程等
        // 「池化线程 + 不清理上下文」组合会跨请求串数据的原因，使用方必须在任务结束时显式 clear()
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            HttpContext first = HttpContext.builder().uri("/first").build();
            HttpContextHolder.setContext(first);

            assertThat(pool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isSameAs(first);

            HttpContextHolder.clear();

            assertThat(pool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isSameAs(first);
        } finally {
            shutdown(pool);
        }
    }

    @Test
    @DisplayName("同样场景下 TTL 包装的池可纠正：clear 之后提交的任务读到 null")
    void ttlWrapperNeutralisesWorkerInheritance() throws Exception {
        // TTL 在提交时捕获、任务内替换、任务后还原，因此即便 worker 已被污染，
        // 每个任务看到的仍是「提交时」的上下文 —— 这是给自建线程池套 TtlExecutors 的价值
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
        try {
            HttpContextHolder.setContext(HttpContext.builder().uri("/first").build());
            ttlPool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS);

            HttpContextHolder.clear();

            assertThat(ttlPool.submit(HttpContextHolder::getContext).get(10, TimeUnit.SECONDS)).isNull();
        } finally {
            shutdown(pool);
        }
    }

    @Test
    @DisplayName("子线程与父线程持有同一实例：子线程的修改对父线程可见（共享而非拷贝）")
    void childThreadMutatesSameInstance() throws Exception {
        ExecutorService pool = preExistingPool();
        ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
        try {
            HttpContextHolder.setContext(sample());

            ttlPool.submit(() -> HttpContextHolder.getContext().setIp("9.9.9.9")).get(10, TimeUnit.SECONDS);

            assertThat(HttpContextHolder.getContext().getIp()).isEqualTo("9.9.9.9");
        } finally {
            shutdown(pool);
        }
    }
}
