package com.snowdrift.framework.context.security;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityContextHolderTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 安全上下文 Holder 单元测试（含 TTL 跨线程传递语义）
 * @since 1.0.0
 */
class SecurityContextHolderTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private static ExecutorService preExistingPool() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> null).get(10, TimeUnit.SECONDS);
        return pool;
    }

    private static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }

    private static SecurityContext loggedIn() {
        return SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员")
                .tenantId(100L).deptId(10L)
                .roleKeys(List.of("admin")).permissions(List.of("sys:user:list"))
                .build();
    }

    @Nested
    @DisplayName("读写与清理")
    class Basic {

        @Test
        @DisplayName("Holder 为 final 且构造器私有（静态工具类约定，不可实例化/继承）")
        void notInstantiable() {
            assertThat(java.lang.reflect.Modifier.isFinal(SecurityContextHolder.class.getModifiers())).isTrue();
            assertThat(SecurityContextHolder.class.getDeclaredConstructors())
                    .isNotEmpty()
                    .allSatisfy(ctor -> assertThat(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers())).isTrue());
        }

        @Test
        void setGetClear() {
            SecurityContext context = loggedIn();

            SecurityContextHolder.setContext(context);
            assertThat(SecurityContextHolder.getContext()).isSameAs(context);
            assertThat(SecurityContextHolder.peekContext()).isSameAs(context);

            SecurityContextHolder.clear();
            assertThat(SecurityContextHolder.peekContext()).isNull();
        }

        @Test
        @DisplayName("getContext 无上下文时抛 BizException，peekContext 返回 null")
        void throwingVersusPeeking() {
            assertThat(SecurityContextHolder.peekContext()).isNull();

            assertThatThrownBy(SecurityContextHolder::getContext)
                    .isInstanceOf(BizException.class)
                    .hasMessage("未获取到安全上下文，请确认请求已登录且安全上下文已注入");
        }

        @Test
        @DisplayName("两个 getter 在同一个上下文下返回同一实例")
        void bothGettersReturnSameInstance() {
            SecurityContext context = loggedIn();
            SecurityContextHolder.setContext(context);

            assertThat(SecurityContextHolder.getContext()).isSameAs(SecurityContextHolder.peekContext());
        }

        @Test
        void nullContextRejected() {
            assertThatThrownBy(() -> SecurityContextHolder.setContext(null))
                    .isInstanceOf(BizException.class)
                    .hasMessage("安全上下文不能为空");
        }

        @Test
        @DisplayName("clear 可重复调用")
        void clearIsIdempotent() {
            SecurityContextHolder.setContext(loggedIn());

            SecurityContextHolder.clear();
            SecurityContextHolder.clear();

            assertThat(SecurityContextHolder.peekContext()).isNull();
        }
    }

    @Nested
    @DisplayName("便捷读取方法")
    class Convenience {

        @Test
        @DisplayName("已登录时返回上下文中的字段")
        void readsFromContext() {
            SecurityContextHolder.setContext(loggedIn());

            assertThat(SecurityContextHolder.getUserId()).isEqualTo(1L);
            assertThat(SecurityContextHolder.getUsername()).isEqualTo("admin");
            assertThat(SecurityContextHolder.getNickname()).isEqualTo("管理员");
            assertThat(SecurityContextHolder.getTenantId()).isEqualTo(100L);
            assertThat(SecurityContextHolder.getDeptId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("未登录时全部抛 BizException（依赖 getContext）")
        void throwsWhenAbsent() {
            assertThatThrownBy(SecurityContextHolder::getUserId).isInstanceOf(BizException.class);
            assertThatThrownBy(SecurityContextHolder::getUsername).isInstanceOf(BizException.class);
            assertThatThrownBy(SecurityContextHolder::getNickname).isInstanceOf(BizException.class);
            assertThatThrownBy(SecurityContextHolder::getTenantId).isInstanceOf(BizException.class);
            assertThatThrownBy(SecurityContextHolder::getDeptId).isInstanceOf(BizException.class);
            assertThatThrownBy(SecurityContextHolder::getOperator).isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("字段缺失时返回 null，不抛异常")
        void nullFieldsAreReturned() {
            SecurityContextHolder.setContext(SecurityContext.builder().username("admin").build());

            assertThat(SecurityContextHolder.getUserId()).isNull();
            assertThat(SecurityContextHolder.getNickname()).isNull();
            assertThat(SecurityContextHolder.getTenantId()).isNull();
            assertThat(SecurityContextHolder.getDeptId()).isNull();
        }

        @Test
        @DisplayName("getOperator：昵称优先，昵称为空回落账号")
        void operatorPrefersNickname() {
            SecurityContextHolder.setContext(SecurityContext.builder()
                    .username("admin").nickname("管理员").build());
            assertThat(SecurityContextHolder.getOperator()).isEqualTo("管理员");

            SecurityContextHolder.setContext(SecurityContext.builder()
                    .username("admin").nickname("  ").build());
            assertThat(SecurityContextHolder.getOperator()).isEqualTo("admin");

            SecurityContextHolder.setContext(SecurityContext.builder().username("admin").build());
            assertThat(SecurityContextHolder.getOperator()).isEqualTo("admin");
        }

        @Test
        @DisplayName("getOperator：昵称与账号都为空时返回 null（调用方需自行兜底）")
        void operatorIsNullWhenBothBlank() {
            SecurityContextHolder.setContext(SecurityContext.builder().build());

            assertThat(SecurityContextHolder.getOperator()).isNull();
        }
    }

    @Nested
    @DisplayName("跨线程传递")
    class Transmission {

        @Test
        @DisplayName("未包装的线程池拿不到上下文")
        void notTransmittedWithoutTtlWrapper() throws Exception {
            ExecutorService pool = preExistingPool();
            try {
                SecurityContextHolder.setContext(loggedIn());

                assertThat(pool.submit(SecurityContextHolder::peekContext).get(10, TimeUnit.SECONDS)).isNull();
            } finally {
                shutdown(pool);
            }
        }

        @Test
        @DisplayName("TTL 包装的线程池可拿到同一实例")
        void transmittedToTtlWrappedPool() throws Exception {
            ExecutorService pool = preExistingPool();
            ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
            try {
                SecurityContext context = loggedIn();
                SecurityContextHolder.setContext(context);

                assertThat(ttlPool.submit(SecurityContextHolder::getContext).get(10, TimeUnit.SECONDS))
                        .isSameAs(context);
            } finally {
                shutdown(pool);
            }
        }

        @Test
        @DisplayName("clear 之后不再传递")
        void clearedContextIsNotTransmitted() throws Exception {
            ExecutorService pool = preExistingPool();
            ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
            try {
                SecurityContextHolder.setContext(loggedIn());
                SecurityContextHolder.clear();

                assertThat(ttlPool.submit(SecurityContextHolder::peekContext).get(10, TimeUnit.SECONDS)).isNull();
            } finally {
                shutdown(pool);
            }
        }

        @Test
        @DisplayName("池内任务结束后，其写入不会残留到该线程的下一个任务")
        void poolTaskMutationDoesNotLeakToNextTask() throws Exception {
            ExecutorService pool = preExistingPool();
            ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
            try {
                ttlPool.submit(() -> SecurityContextHolder.setContext(loggedIn())).get(10, TimeUnit.SECONDS);

                assertThat(ttlPool.submit(SecurityContextHolder::peekContext).get(10, TimeUnit.SECONDS)).isNull();
            } finally {
                shutdown(pool);
            }
        }

        @Test
        @DisplayName("子线程与父线程共享同一可变实例：子线程修改字段对父线程可见")
        void childThreadMutatesSameInstance() throws Exception {
            ExecutorService pool = preExistingPool();
            ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(pool);
            try {
                SecurityContextHolder.setContext(loggedIn());

                ttlPool.submit(() -> SecurityContextHolder.getContext().setTenantId(999L)).get(10, TimeUnit.SECONDS);

                assertThat(SecurityContextHolder.getTenantId()).isEqualTo(999L);
            } finally {
                shutdown(pool);
            }
        }
    }
}
