package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DistributedLockAspectTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 分布式锁切面单元测试
 * @since 1.0.0
 */
class DistributedLockAspectTest {

    /** 记录加锁/解锁调用的假实现 */
    static class RecordingLockService implements IDistributedLockService {
        final List<String> events = new ArrayList<>();
        boolean lockResult = true;
        RuntimeException unlockFailure;

        @Override
        public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
            events.add("tryLock:" + key + ":" + waitTime + ":" + leaseTime + ":" + unit);
            return lockResult;
        }

        @Override
        public void unlock(String key) {
            events.add("unlock:" + key);
            if (unlockFailure != null) {
                throw unlockFailure;
            }
        }

        @Override
        public boolean isLocked(String key) {
            return false;
        }

        @Override
        public void forceUnlock(String key) {
        }

        @Override
        public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unused")
    static class Annotated {
        @DistributedLock(key = "'order:pay:' + #orderId", waitTime = 3, leaseTime = 10)
        public String pay(Long orderId) {
            return "paid";
        }

        @DistributedLock(key = "#orderId", message = "订单处理中，请稍后重试")
        public String customMessage(Long orderId) {
            return "paid";
        }

        @DistributedLock(key = " ")
        public String blankKey(Long orderId) {
            return "paid";
        }
    }

    private static DistributedLock anno(String methodName) throws Exception {
        Method method = Annotated.class.getMethod(methodName, Long.class);
        return method.getAnnotation(DistributedLock.class);
    }

    private static ProceedingJoinPoint joinPoint(String methodName, Object[] args) throws Exception {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Annotated.class.getMethod(methodName, Long.class));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private static ProceedingJoinPoint succeeding(String methodName, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(methodName, args);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    private static ProceedingJoinPoint failing(String methodName, Object[] args, Throwable error) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(methodName, args);
        when(joinPoint.proceed()).thenThrow(error);
        return joinPoint;
    }

    @Test
    @DisplayName("加锁成功：执行业务并在 finally 释放锁，waitTime/leaseTime 透传")
    void locksAndReleases() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);
        ProceedingJoinPoint joinPoint = succeeding("pay", new Object[]{1L}, "paid");

        Object result = aspect.around(joinPoint, anno("pay"));

        assertThat(result).isEqualTo("paid");
        assertThat(lockService.events).containsExactly(
                "tryLock:order:pay:1:3:10:SECONDS", "unlock:order:pay:1");
    }

    @Test
    @DisplayName("加锁失败：抛 LOCK_FAILED 业务码 + 注解 message，不执行业务、不释放锁")
    void lockFailureFailsFast() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        lockService.lockResult = false;
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);
        ProceedingJoinPoint joinPoint = succeeding("pay", new Object[]{1L}, "paid");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("pay")))
                .isInstanceOf(BizException.class)
                .hasMessage("操作正在处理中，请勿重复提交")
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ResultCode.LOCK_FAILED.code());

        verify(joinPoint, never()).proceed();
        assertThat(lockService.events).hasSize(1);
    }

    @Test
    @DisplayName("加锁失败：使用注解自定义 message")
    void customFailureMessage() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        lockService.lockResult = false;
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);

        assertThatThrownBy(() -> aspect.around(succeeding("customMessage", new Object[]{1L}, "paid"),
                anno("customMessage")))
                .isInstanceOf(BizException.class)
                .hasMessage("订单处理中，请稍后重试");
    }

    @Test
    @DisplayName("业务抛异常：锁仍在 finally 释放，原异常照常抛出")
    void releasesLockOnBusinessFailure() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);
        ProceedingJoinPoint joinPoint = failing("pay", new Object[]{1L}, new IllegalStateException("业务失败"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("pay")))
                .isInstanceOf(IllegalStateException.class).hasMessage("业务失败");

        assertThat(lockService.events).containsExactly(
                "tryLock:order:pay:1:3:10:SECONDS", "unlock:order:pay:1");
    }

    @Test
    @DisplayName("业务抛 Error：锁同样释放（finally 语义）")
    void releasesLockOnError() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);
        ProceedingJoinPoint joinPoint = failing("pay", new Object[]{1L}, new AssertionError("断言失败"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("pay")))
                .isInstanceOf(AssertionError.class);

        assertThat(lockService.events).last().isEqualTo("unlock:order:pay:1");
    }

    @Test
    @DisplayName("释放锁失败：仅记日志，不掩盖业务结果")
    void unlockFailureDoesNotMaskResult() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        lockService.unlockFailure = new IllegalStateException("释放失败");
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);

        Object result = aspect.around(succeeding("pay", new Object[]{1L}, "paid"), anno("pay"));

        assertThat(result).isEqualTo("paid");
    }

    @Test
    @DisplayName("SpEL 解析不出 lock key（空白）时快速失败，避免退化成全局锁")
    void blankKeyFailsFast() throws Throwable {
        RecordingLockService lockService = new RecordingLockService();
        DistributedLockAspect aspect = new DistributedLockAspect(lockService);
        ProceedingJoinPoint joinPoint = succeeding("blankKey", new Object[]{1L}, "paid");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("blankKey")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("SpEL 锁 key 解析失败");

        assertThat(lockService.events).isEmpty();
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("缺少分布式锁后端：注解方法 fail-closed，不执行业务")
    void missingLockBackendFailsClosed() throws Throwable {
        DistributedLockAspect aspect = new DistributedLockAspect(null);
        ProceedingJoinPoint joinPoint = succeeding("pay", new Object[]{1L}, "paid");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno("pay")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("IDistributedLockService");

        verify(joinPoint, never()).proceed();
    }
}
