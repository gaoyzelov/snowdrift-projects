package com.snowdrift.cache.redis.service;

import lombok.NonNull;
import org.redisson.api.RedissonClient;

/**
 * IRedisLockService
 *
 * @author gaoye
 * @date 2025/03/20 10:18:20
 * @description  Redis分布式锁服务接口
 * @since 1.0.0
 */
public interface IRedisLockService {

    RedissonClient getRedissonClient();

    void lock(String lockKey);

    void lock(String lockKey, long lockTime);

    boolean tryLock(String lockKey);

    boolean tryLock(String lockKey, long waitTime);

    boolean tryLock(String lockKey, long lockTime, long waitTime);

    void unlock(String lockKey);

    boolean isLock(String lockKey);

    int getHoldCount(@NonNull String lockKey);

    long getLockTtl(String lockKey);

    boolean isHeldByCurrentThread(String lockKey);
}