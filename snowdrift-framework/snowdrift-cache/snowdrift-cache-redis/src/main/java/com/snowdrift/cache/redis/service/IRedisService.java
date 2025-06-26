package com.snowdrift.cache.redis.service;
import com.snowdrift.cache.redis.exception.LockException;
import java.util.concurrent.TimeUnit;

/**
 * IRedisService
 *
 * @author gaoye
 * @date 2025/06/26 14:03:59
 * @description xxxxxxxx
 * @since 1.0
 */
public interface IRedisService {

    void lock(String key);

    default void lock(String key,long leaseTime){
        lock(key, leaseTime, TimeUnit.SECONDS);
    }

    void lock(String key,long leaseTime,TimeUnit timeUnit);

    boolean tryLock(String key);

    default boolean tryLock(String key, long waitTime) throws LockException{
        return tryLock(key, waitTime, TimeUnit.SECONDS);
    }

    boolean tryLock(String key, long waitTime, TimeUnit timeUnit) throws LockException;

    default boolean tryLock(String key, long waitTime, long leaseTime) throws LockException{
        return tryLock(key, waitTime, leaseTime, TimeUnit.SECONDS);
    }

    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws LockException;

    void unlock(String key);

    boolean isLocked(String key);

    boolean isHeldByCurrentThread(String key);
}