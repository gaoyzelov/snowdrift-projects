package com.snowdrift.cache.redis.service.impl;

import com.snowdrift.cache.redis.service.IRedisLockService;
import lombok.NonNull;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * RedisLockServiceImpl
 *
 * @author gaoye
 * @date 2025/03/20 10:19:05
 * @description Redis 分布式锁服务实现类
 * @since 1.0.0
 */
public class RedisLockServiceImpl implements IRedisLockService {

    private final RedissonClient redissonClient;

    public RedisLockServiceImpl(@NonNull RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取 RedissonClient
     *
     * @return RedissonClient
     */
    @Override
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * 加锁
     *
     * @param lockKey 锁键
     */
    @Override
    public void lock(@NonNull String lockKey) {
        redissonClient.getLock(lockKey).lock();
    }

    /**
     * 加锁
     *
     * @param lockKey  锁键
     * @param lockTime 锁时间
     */
    @Override
    public void lock(@NonNull String lockKey, long lockTime) {
        redissonClient.getLock(lockKey).lock(lockTime, TimeUnit.SECONDS);
    }

    /**
     * 尝试加锁
     *
     * @param lockKey 锁键
     * @return 是否加锁成功
     */
    @Override
    public boolean tryLock(@NonNull String lockKey) {
        return redissonClient.getLock(lockKey).tryLock();
    }

    /**
     * 尝试加锁
     *
     * @param lockKey  锁键
     * @param waitTime 等待时间
     * @return 是否加锁成功
     */
    @Override
    public boolean tryLock(@NonNull String lockKey, long waitTime) {
        try {
            return redissonClient.getLock(lockKey).tryLock(waitTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 尝试加锁
     *
     * @param lockKey  锁键
     * @param lockTime 锁时间
     * @param waitTime 等待时间
     * @return 是否加锁成功
     */
    @Override
    public boolean tryLock(@NonNull String lockKey, long lockTime, long waitTime) {
        try {
            return redissonClient.getLock(lockKey).tryLock(waitTime, lockTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 解锁
     *
     * @param lockKey 锁键
     */
    @Override
    public void unlock(@NonNull String lockKey) {
        redissonClient.getLock(lockKey).unlock();
    }

    /**
     * 判断是否被锁
     *
     * @param lockKey 锁键
     * @return 是否被锁
     */
    @Override
    public boolean isLock(@NonNull String lockKey) {
        return redissonClient.getLock(lockKey).isLocked();
    }

    /**
     * 获取线程持有锁的次数
     *
     * @param lockKey 锁键
     * @return 锁持有次数
     */
    @Override
    public int getHoldCount(@NonNull String lockKey) {
        return redissonClient.getLock(lockKey).getHoldCount();
    }

    /**
     * 获取锁剩余时间
     *
     * @param lockKey 锁键
     * @return 锁剩余时间
     */
    @Override
    public long getLockTtl(@NonNull String lockKey) {
        return redissonClient.getLock(lockKey).remainTimeToLive();
    }

    /**
     * 判断是否由当前线程持有锁
     *
     * @param lockKey 锁键
     * @return 是否由当前线程持有锁
     */
    @Override
    public boolean isHeldByCurrentThread(@NonNull String lockKey) {
        return redissonClient.getLock(lockKey).isHeldByCurrentThread();
    }
}