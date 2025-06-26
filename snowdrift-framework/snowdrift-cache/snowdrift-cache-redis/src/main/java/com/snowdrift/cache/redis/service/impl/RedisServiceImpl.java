package com.snowdrift.cache.redis.service.impl;

import com.snowdrift.cache.redis.service.IRedisService;
import com.snowdrift.cache.redis.exception.LockException;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * RedisServiceImpl
 *
 * @author gaoye
 * @date 2025/06/26 14:04:24
 * @description xxxxxxxx
 * @since 1.0
 */
public class RedisServiceImpl implements IRedisService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void lock(String key) {
        redissonClient.getLock(key).lock();
    }

    @Override
    public void lock(String key, long leaseTime, TimeUnit timeUnit) {
        redissonClient.getLock(key).lock(leaseTime, timeUnit);
    }

    @Override
    public boolean tryLock(String key) {
        return redissonClient.getLock(key).tryLock();
    }

    @Override
    public boolean tryLock(String key, long waitTime, TimeUnit timeUnit) throws LockException {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, timeUnit);
        } catch (Exception e) {
            throw new LockException(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws LockException {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, leaseTime, timeUnit);
        } catch (Exception e) {
            throw new LockException(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }

    @Override
    public boolean isHeldByCurrentThread(String key) {
        return redissonClient.getLock(key).isHeldByCurrentThread();
    }

    @Override
    public void unlock(String key) {
        redissonClient.getLock(key).unlock();
    }
}