package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class RedisCacheServiceImpl extends AbstractCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisCacheServiceImpl(CacheProperties properties,
                                  CacheSerializer serializer,
                                  RedisTemplate<String, String> redisTemplate) {
        super(serializer);
        AssertUtil.notNull(properties, "cache.config.required");
        AssertUtil.notNull(redisTemplate, "cache.redis.template.required");

        this.redisTemplate = redisTemplate;
        setKeyPrefix(properties.getKeyPrefix());
        setDefaultTtl(properties.getKeyTtl());
    }

    // =================== ICacheService 实现 ===================

    @Override
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "cache.key.required");
        String json = redisTemplate.opsForValue().get(buildKey(key));
        if (json == null) {
            return null;
        }
        return deserialize(json, type);
    }

    @Override
    public void put(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        redisTemplate.opsForValue().set(buildKey(key), serialize(value));
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        Duration effectiveTtl = effectiveTtl(ttl);
        String realKey = buildKey(key);
        if (effectiveTtl != null) {
            redisTemplate.opsForValue().set(realKey, serialize(value), effectiveTtl);
        } else {
            redisTemplate.opsForValue().set(realKey, serialize(value));
        }
    }

    @Override
    public boolean putIfAbsent(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        String realKey = buildKey(key);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(realKey, serialize(value));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        Duration effectiveTtl = effectiveTtl(ttl);
        String realKey = buildKey(key);
        Boolean result;
        if (effectiveTtl != null) {
            result = redisTemplate.opsForValue().setIfAbsent(realKey, serialize(value), effectiveTtl);
        } else {
            result = redisTemplate.opsForValue().setIfAbsent(realKey, serialize(value));
        }
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean delete(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return redisTemplate.delete(buildKey(key));
    }

    @Override
    public long delete(Collection<String> keys) {
        AssertUtil.notNull(keys, "cache.keys.required");
        if (keys.isEmpty()) {
            return 0;
        }
        Set<String> realKeys = keys.stream()
                .map(this::buildKey)
                .collect(Collectors.toSet());
        return redisTemplate.delete(realKeys);
    }

    @Override
    public boolean exists(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return redisTemplate.hasKey(buildKey(key));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(ttl, "cache.ttl.required");
        return Boolean.TRUE.equals(redisTemplate.expire(buildKey(key), ttl));
    }

    @Override
    public long getExpire(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return redisTemplate.getExpire(buildKey(key), TimeUnit.SECONDS);
    }

    @Override
    public Set<String> keys(String pattern) {
        AssertUtil.notBlank(pattern, "cache.pattern.required");
        String realPattern = buildKey(pattern);
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(realPattern).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 去掉 key 前缀
                if (StringUtils.isNotBlank(keyPrefix) && key.startsWith(keyPrefix)) {
                    result.add(key.substring(keyPrefix.length()));
                } else {
                    result.add(key);
                }
            }
        }
        return result;
    }
}
