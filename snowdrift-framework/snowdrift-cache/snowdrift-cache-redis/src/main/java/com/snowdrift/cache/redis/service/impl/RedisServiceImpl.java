package com.snowdrift.cache.redis.service.impl;

import com.snowdrift.cache.redis.exception.LockException;
import com.snowdrift.cache.redis.service.IRedisService;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 阻塞加锁
     *
     * @param key 锁的key
     */
    @Override
    public void lock(String key) {
        redissonClient.getLock(key).lock();
    }

    /**
     * 阻塞加锁
     *
     * @param key       锁的key
     * @param leaseTime 锁的过期时间
     * @param timeUnit  时间单位
     */
    @Override
    public void lock(String key, long leaseTime, TimeUnit timeUnit) {
        redissonClient.getLock(key).lock(leaseTime, timeUnit);
    }

    /**
     * 尝试加锁
     *
     * @param key 锁的key
     * @return boolean
     */
    @Override
    public boolean tryLock(String key) {
        return redissonClient.getLock(key).tryLock();
    }

    /**
     * 尝试加锁
     *
     * @param key      锁的key
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @return boolean
     */
    @Override
    public boolean tryLock(String key, long waitTime, TimeUnit timeUnit) throws LockException {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, timeUnit);
        } catch (Exception e) {
            throw new LockException(e.getLocalizedMessage());
        }
    }

    /**
     * 尝试加锁
     *
     * @param key       锁的key
     * @param waitTime  尝试加锁等待时间
     * @param leaseTime 锁的过期时间
     * @param timeUnit  时间单位
     * @return boolean
     */
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws LockException {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, leaseTime, timeUnit);
        } catch (Exception e) {
            throw new LockException(e.getLocalizedMessage());
        }
    }

    /**
     * 键是否被锁定
     *
     * @param key 锁的key
     * @return boolean
     */
    @Override
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }

    /**
     * 判断当前线程是否持有锁
     *
     * @param key 锁的key
     * @return boolean
     */
    @Override
    public boolean isHeldByCurrentThread(String key) {
        return redissonClient.getLock(key).isHeldByCurrentThread();
    }

    /**
     * 释放锁
     *
     * @param key 锁的key
     */
    @Override
    public void unlock(String key) {
        unlock(key, false);
    }

    /**
     * 释放锁
     *
     * @param key   锁的key
     * @param force 强制释放锁
     */
    @Override
    public void unlock(String key, boolean force) {
        if (force) {
            redissonClient.getLock(key).forceUnlock();
        } else {
            redissonClient.getLock(key).unlock();
        }
    }

    /**
     * 设置缓存
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 带过期时间的设置缓存
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    @Override
    public void setEx(String key, Object value, Duration timout) {
        redisTemplate.opsForValue().set(key, value, timout);
    }

    /**
     * 获取缓存
     *
     * @param key 缓存的key
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object o = redisTemplate.opsForValue().get(key);
        if (o == null) {
            return null;
        }
        return clazz.cast(o);
    }

    /**
     * 获取缓存的剩余时间
     *
     * @param key 缓存的key
     */
    @Override
    public Long getTtl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 如果key不存在，则添加缓存
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    @Override
    public Boolean setNx(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 带过期时间的设置缓存且当键不存在时
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    @Override
    public Boolean setNxEx(String key, Object value, Duration timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 设置缓存的bit位
     *
     * @param key    缓存的key
     * @param offset bit位置
     * @param value  bit值
     */
    @Override
    public Boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 获取缓存的bit位
     *
     * @param key    缓存的key
     * @param offset bit位置
     */
    @Override
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 获取缓存的bit位数量
     *
     * @param key 缓存的key
     */
    @Override
    public Long bitCount(String key) {
        return redisTemplate.execute((RedisCallback<Long>) conn -> conn.bitCount(key.getBytes()));
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存的key
     */
    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存的key
     */
    @Override
    public Boolean delKey(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存的key
     */
    @Override
    public Long delKeys(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 获取缓存的递增值
     *
     * @param key 缓存的key
     */
    @Override
    public Long getIncr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 获取缓存的递增值
     *
     * @param key   缓存的key
     * @param delta 递增的值
     */
    @Override
    public Long getIncr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 获取缓存的递减值
     *
     * @param key 缓存的key
     */
    @Override
    public Long getDecr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 获取缓存的递减值
     *
     * @param key   缓存的key
     * @param delta 递减的值
     */
    @Override
    public Long getDecr(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 设置缓存的hash值
     *
     * @param key     缓存的key
     * @param hashKey hash的key
     * @param value   hash的值
     */
    @Override
    public void hashSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 添加缓存的hash值且当键不存在时
     *
     * @param key     缓存的key
     * @param hashKey hash的key
     * @param value   hash的值
     */
    @Override
    public Boolean hashSetNx(String key, String hashKey, Object value) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    /**
     * 批量设置缓存的hash值
     *
     * @param key 缓存的key
     * @param map hash的key和value
     */
    @Override
    public void hashSetBatch(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取缓存的hash自增值
     *
     * @param key     缓存的key
     * @param hashKey hash的key
     * @param delta   自增的值
     */
    @Override
    public Long hashGetIncr(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 获取缓存的hash值
     *
     * @param key     缓存的key
     * @param hashKey hash的key
     * @param clazz   缓存的值的类型
     */
    @Override
    public <T> T hashGet(String key, String hashKey, Class<T> clazz) {
        Object o = redisTemplate.opsForHash().get(key, hashKey);
        return Objects.isNull(o) ? null : clazz.cast(o);
    }

    /**
     * 获取缓存的hash所有值
     *
     * @param key 缓存的key
     */
    @Override
    public Map<Object,Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }
}