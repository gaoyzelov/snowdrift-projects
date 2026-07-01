package com.snowdrift.framework.cache.redisson.service;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.result.ResultCode;
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
public class RedissonLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    public RedissonLockService(RedissonClient redissonClient) {
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
            return false;
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
            throw new BizException(ResultCode.ERR.code(), "cache.lock.failed");
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }
}
