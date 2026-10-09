package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SnowdriftRedissonDistributedLockServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Redisson 分布式锁实现单元测试（mock RedissonClient/RLock）
 * @since 1.0.0
 */
class SnowdriftRedissonDistributedLockServiceImplTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final SnowdriftRedissonDistributedLockServiceImpl lockService =
            new SnowdriftRedissonDistributedLockServiceImpl(redissonClient);

    private RLock lock(String key) {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(key)).thenReturn(lock);
        return lock;
    }

    @Test
    @DisplayName("tryLock：透传等待时间/持有时间并返回 Redisson 结果")
    void tryLock() throws Exception {
        RLock lock = lock("order:1");
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(lockService.tryLock("order:1", 3, 10, TimeUnit.SECONDS)).isTrue();
        verify(lock).tryLock(3, 10, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("tryLock 被中断：恢复中断标记并抛 BizException，与“锁竞争失败”区分")
    void tryLockInterrupted() throws Exception {
        RLock lock = lock("order:1");
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenThrow(new InterruptedException("中断"));

        try {
            assertThatThrownBy(() -> lockService.tryLock("order:1", 3, 10, TimeUnit.SECONDS))
                    .isInstanceOf(BizException.class)
                    .hasMessage("获取分布式锁被中断");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("unlock：仅当锁由当前线程持有时释放，避免 IllegalMonitorStateException")
    void unlockOnlyWhenHeld() {
        RLock held = lock("order:1");
        when(held.isHeldByCurrentThread()).thenReturn(true);
        lockService.unlock("order:1");
        verify(held).unlock();

        RLock notHeld = lock("order:2");
        when(notHeld.isHeldByCurrentThread()).thenReturn(false);
        lockService.unlock("order:2");
        verify(notHeld, never()).unlock();
    }

    @Test
    @DisplayName("isLocked / forceUnlock 透传 Redisson")
    void otherOperations() {
        RLock lock = lock("order:1");
        when(lock.isLocked()).thenReturn(true);

        assertThat(lockService.isLocked("order:1")).isTrue();

        lockService.forceUnlock("order:1");
        verify(lock).forceUnlock();
    }

    @Test
    @DisplayName("executeWithLock：加锁成功执行并释放；加锁失败抛 LOCK_FAILED 且不执行业务")
    void executeWithLock() throws Exception {
        RLock lock = lock("order:1");
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThat(lockService.executeWithLock("order:1", 1, 5, TimeUnit.SECONDS, () -> "ok")).isEqualTo("ok");
        verify(lock).unlock();

        RLock busy = lock("order:2");
        when(busy.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> lockService.executeWithLock("order:2", 1, 5, TimeUnit.SECONDS, () -> "never"))
                .isInstanceOf(BizException.class)
                .hasMessage(ResultCode.LOCK_FAILED.msg());
    }

    @Test
    @DisplayName("executeWithLock：业务抛异常时仍释放锁")
    void executeWithLockReleasesOnFailure() throws Exception {
        RLock lock = lock("order:1");
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> lockService.executeWithLock("order:1", 1, 5, TimeUnit.SECONDS, () -> {
            throw new IllegalStateException("业务失败");
        })).isInstanceOf(IllegalStateException.class);

        verify(lock).unlock();
    }
}
