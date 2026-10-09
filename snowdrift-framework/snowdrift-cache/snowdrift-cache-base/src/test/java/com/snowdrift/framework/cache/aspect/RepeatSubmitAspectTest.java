package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.annotation.RepeatSubmit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RepeatSubmitAspectTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 防重复提交切面单元测试
 * @since 1.0.0
 */
class RepeatSubmitAspectTest {

    /** 可观察的内存缓存实现：记录 putIfAbsent 的 TTL，可模拟删除失败 */
    static class RecordingCache implements ICacheService {
        final Map<String, String> store = new HashMap<>();
        Duration lastTtl;
        boolean failOnDelete;

        @Override
        public boolean putIfAbsent(String key, Object value, Duration ttl) {
            lastTtl = ttl;
            return store.putIfAbsent(key, (String) value) == null;
        }

        @Override
        public <T> T get(String key, Class<T> type) {
            String value = store.get(key);
            return value == null ? null : type.cast(value);
        }

        @Override
        public boolean delete(String key) {
            if (failOnDelete) {
                throw new IllegalStateException("删除失败");
            }
            return store.remove(key) != null;
        }

        @Override
        public <T> T hget(String key, String hashKey, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(String key, java.lang.reflect.Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T hget(String key, String hashKey, java.lang.reflect.Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void hput(String key, String hashKey, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putIfAbsent(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void hdelete(String key, String hashKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long batchDelete(Collection<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean expire(String key, Duration ttl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getExpire(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long increment(String key, Duration ttl) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unused")
    static class Annotated {
        @RepeatSubmit(key = "'order:' + #orderNo", interval = 30)
        public String create(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "#orderNo")
        public String keyFromParam(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = " ")
        public String blankKey(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "static:fixed")
        public String fixedKey(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "#orderNo", interval = 0)
        public String zeroInterval(String orderNo) {
            return "created";
        }
    }

    private RecordingCache cache;
    private RepeatSubmitAspect aspect;

    @BeforeEach
    void setUp() {
        cache = new RecordingCache();
        aspect = new RepeatSubmitAspect(cache);
    }

    private static RepeatSubmit anno(String methodName) throws Exception {
        Method method = Annotated.class.getMethod(methodName, String.class);
        return method.getAnnotation(RepeatSubmit.class);
    }

    /** 构造带真实方法签名的切点：SpEL 解析依赖 MethodSignature#getMethod() */
    private static ProceedingJoinPoint joinPoint(String methodName, Object[] args) throws Exception {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Annotated.class.getMethod(methodName, String.class));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    /** 业务正常返回的切点 */
    private static ProceedingJoinPoint succeeding(String methodName, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(methodName, args);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    /** 业务抛异常的切点 */
    private static ProceedingJoinPoint failing(String methodName, Object[] args, Throwable error) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(methodName, args);
        when(joinPoint.proceed()).thenThrow(error);
        return joinPoint;
    }

    @Test
    @DisplayName("首次提交：写入标记并执行业务，TTL 取注解 interval")
    void firstSubmitProceeds() throws Throwable {
        ProceedingJoinPoint joinPoint = succeeding("create", new Object[]{"A001"}, "ok");

        Object result = aspect.around(joinPoint, anno("create"));

        assertThat(result).isEqualTo("ok");
        assertThat(cache.store).containsKey("order:A001");
        assertThat(cache.lastTtl).isEqualTo(Duration.of(30, ChronoUnit.SECONDS));
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("时间窗口内重复提交：抛 BizException 且不执行业务")
    void duplicateSubmitRejected() throws Throwable {
        cache.store.put("order:A001", "previous-nonce");
        ProceedingJoinPoint joinPoint = succeeding("create", new Object[]{"A001"}, "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("create")))
                .isInstanceOf(BizException.class)
                .hasMessage("请勿重复提交，请稍后重试");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("key 为 #参数 形式时用参数值作为 key")
    void keyFromSingleParameter() throws Throwable {
        ProceedingJoinPoint joinPoint = succeeding("keyFromParam", new Object[]{"A002"}, "ok");

        assertThat(aspect.around(joinPoint, anno("keyFromParam"))).isEqualTo("ok");
        assertThat(cache.store).containsKey("A002");
    }

    @Test
    @DisplayName("key 不含 # 时按静态 key 原样使用")
    void staticKey() throws Throwable {
        ProceedingJoinPoint joinPoint = succeeding("fixedKey", new Object[]{"A002"}, "ok");

        assertThat(aspect.around(joinPoint, anno("fixedKey"))).isEqualTo("ok");
        assertThat(cache.store).containsKey("static:fixed");
    }

    @Test
    @DisplayName("业务抛异常：删除本请求写入的标记，允许修正后重新提交，原异常照常抛出")
    void businessFailureClearsMarker() throws Throwable {
        ProceedingJoinPoint joinPoint = failing("create", new Object[]{"A001"}, new IllegalStateException("业务失败"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("create")))
                .isInstanceOf(IllegalStateException.class).hasMessage("业务失败");

        assertThat(cache.store).doesNotContainKey("order:A001");
    }

    @Test
    @DisplayName("缓存中的标记已不是本请求写入的 nonce（已过期并被并发请求重写）：不删除")
    void doesNotDeleteForeignMarker() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("create", new Object[]{"A001"});
        AtomicReference<String> rewritten = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            // 模拟执行期间本请求标记过期、被其它请求写入新 nonce
            rewritten.set("other-nonce");
            cache.store.put("order:A001", "other-nonce");
            throw new IllegalStateException("业务失败");
        });

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("create")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(cache.store).containsEntry("order:A001", rewritten.get());
    }

    @Test
    @DisplayName("标记删除失败：仅记日志，不覆盖原始业务异常")
    void deleteFailureDoesNotMaskBusinessException() throws Throwable {
        cache.failOnDelete = true;
        ProceedingJoinPoint joinPoint = failing("create", new Object[]{"A001"},
                new IllegalArgumentException("原始异常"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("create")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("原始异常");
    }

    @Test
    @DisplayName("业务抛中断异常：恢复中断标记后原样抛出")
    void interruptedRestoresFlag() throws Throwable {
        AtomicBoolean flagRestored = new AtomicBoolean();
        ProceedingJoinPoint joinPoint = failing("create", new Object[]{"A001"}, new InterruptedException("被中断"));

        try {
            aspect.around(joinPoint, anno("create"));
        } catch (InterruptedException e) {
            flagRestored.set(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertThat(flagRestored).isTrue();
    }

    @Test
    @DisplayName("回归：interval=0 不再静默放行，直接抛 BizException（此前防重窗口为空、重复请求全部通过）")
    void zeroIntervalIsRejected() throws Throwable {
        ProceedingJoinPoint joinPoint = succeeding("zeroInterval", new Object[]{"A001"}, "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("zeroInterval")))
                .isInstanceOf(BizException.class)
                .hasMessage("防重间隔（interval）必须大于 0");
        verify(joinPoint, never()).proceed();
        assertThat(cache.store).isEmpty();
    }

    @Test
    @DisplayName("SpEL 解析不出 key（空白）时快速失败，避免退化成全局防重标记")
    void blankKeyFailsFast() throws Throwable {
        ProceedingJoinPoint joinPoint = succeeding("blankKey", new Object[]{"A001"}, "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("blankKey")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("SpEL 防重 key 解析失败");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("缓存不可用时异常直接向上抛出（基础设施故障调用方需感知）")
    void cacheFailurePropagates() throws Exception {
        ICacheService failingCache = mock(ICacheService.class);
        when(failingCache.putIfAbsent(anyString(), any(), any()))
                .thenThrow(new IllegalStateException("缓存不可用"));
        RepeatSubmitAspect failingAspect = new RepeatSubmitAspect(failingCache);
        ProceedingJoinPoint joinPoint = joinPoint("create", new Object[]{"A001"});

        assertThatThrownBy(() -> failingAspect.around(joinPoint, anno("create")))
                .isInstanceOf(IllegalStateException.class).hasMessage("缓存不可用");
    }
}
