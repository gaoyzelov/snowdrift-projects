package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存实现
 * <p>
 * 基于 {@link RedisTemplate}{@code <String, String>}，统一使用 JSON 字符串存储。
 * 序列化由 {@link CacheSerializer} 统一处理，与 Caffeine / Redisson 后端数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public class SnowdriftRedisCacheServiceImpl extends AbstractCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    public SnowdriftRedisCacheServiceImpl(SnowdriftCacheProperties properties,
                                          CacheSerializer serializer,
                                          RedisTemplate<String, String> redisTemplate) {
        super(properties, serializer);
        this.redisTemplate = redisTemplate;
    }

    // =================== AbstractCacheService 抽象方法实现 ===================

    @Override
    public String doGet(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void doPut(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void doPut(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean doDelete(String key) {
        return redisTemplate.delete(buildKey(key));
    }

    @Override
    public long doBatchDelete(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    public boolean doExists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public boolean doExpire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    @Override
    public long doGetExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
