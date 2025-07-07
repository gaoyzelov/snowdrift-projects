package com.snowdrift.cache.redis.service;
import com.snowdrift.cache.redis.exception.LockException;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
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

    void unlock(String key, boolean force);

    void set(String key, Object value);

    void setEx(String key, Object value, Duration timout);

    <T> T get(String key, Class<T> clazz);

    Long getTtl(String key);

    Boolean setNx(String key, Object value);

    Boolean setNxEx(String key, Object value, Duration timeout);

    Boolean setBit(String key, long offset, boolean value);

    Boolean getBit(String key, long offset);

    Long bitCount(String key);

    Boolean hasKey(String key);

    Boolean delKey(String key);

    Long delKeys(Collection<String> keys);

    Long getIncr(String key);

    Long getIncr(String key, long delta);

    Long getDecr(String key);

    Long getDecr(String key, long delta);

    void hashSet(String key, String hashKey, Object value);

    Boolean hashSetNx(String key, String hashKey, Object value);

    void hashSetBatch(String key, Map<String, Object> map);

    Long hashGetIncr(String key, String hashKey, long delta);

    <T> T hashGet(String key, String hashKey, Class<T> clazz);

    Map<Object,Object> hashGetAll(String key);
}