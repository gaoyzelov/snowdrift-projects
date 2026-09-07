package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁实现
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftRedissonDistributedLockServiceImpl implements IDistributedLockService {

    private final RedissonClient redissonClient;

    public SnowdriftRedissonDistributedLockServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断: key={}", key, e);
            // 中断属取消而非“锁竞争失败”，抛出独立异常以免上层误报“操作处理中，请勿重复提交”
            throw new BizException("获取分布式锁被中断", e);
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }

    @Override
    public void forceUnlock(String key) {
        redissonClient.getLock(key).forceUnlock();
    }

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime,
                                  TimeUnit unit, Supplier<T> supplier) {
        boolean locked = tryLock(key, waitTime, leaseTime, unit);
        if (!locked) {
            throw new BizException(ResultCode.LOCK_FAILED);
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }
}
