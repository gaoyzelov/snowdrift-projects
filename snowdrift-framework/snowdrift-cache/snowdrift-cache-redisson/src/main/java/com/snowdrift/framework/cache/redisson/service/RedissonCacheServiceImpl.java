package com.snowdrift.framework.cache.redisson.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

/**
 * Redisson 缓存实现
 * <p>
 * 基于 {@link RedissonClient}，利用 Redisson 的 RBucket 实现 ICacheService，
 * Jackson 序列化由 Redisson 内置的 Codec 处理。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
public class RedissonCacheServiceImpl extends AbstractCacheService {

    private final RedissonClient redissonClient;

    public RedissonCacheServiceImpl(CacheProperties properties, RedissonClient redissonClient) {
        AssertUtil.notNull(properties, "cache.config.required");
        AssertUtil.notNull(redissonClient, "cache.redisson.client.required");

        this.redissonClient = redissonClient;
        setKeyPrefix(properties.getKeyPrefix());
        setDefaultTtl(properties.getDefaultTtl());
    }

    // =================== ICacheService 实现 ===================

    @Override
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "cache.key.required");
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        Object value = bucket.get();
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        Duration effectiveTtl = effectiveTtl(ttl);
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        if (effectiveTtl != null) {
            bucket.set(value, effectiveTtl);
        } else {
            bucket.set(value);
        }
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        Duration effectiveTtl = effectiveTtl(ttl);
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        if (effectiveTtl != null) {
            return bucket.setIfAbsent(value, effectiveTtl);
        }
        return bucket.setIfAbsent(value);
    }

    @Override
    public boolean delete(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return redissonClient.getBucket(buildKey(key)).delete();
    }

    @Override
    public long delete(Collection<String> keys) {
        AssertUtil.notNull(keys, "cache.keys.required");
        if (keys.isEmpty()) {
            return 0;
        }
        String[] realKeys = keys.stream()
                .map(this::buildKey)
                .toArray(String[]::new);
        return redissonClient.getKeys().delete(realKeys);
    }

    @Override
    public boolean exists(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return redissonClient.getBucket(buildKey(key)).isExists();
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(ttl, "cache.ttl.required");
        return redissonClient.getBucket(buildKey(key)).expire(ttl);
    }

    @Override
    public long getExpire(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        long remain = redissonClient.getBucket(buildKey(key)).remainTimeToLive();
        return remain > 0 ? Duration.ofMillis(remain).toSeconds() : remain;
    }

    @Override
    public Set<String> keys(String pattern) {
        AssertUtil.notBlank(pattern, "cache.pattern.required");
        KeysScanOptions options = KeysScanOptions.defaults().pattern(buildKey(pattern));
        Iterable<String> iterable = redissonClient.getKeys().getKeys(options);
        Set<String> result = new java.util.HashSet<>();
        for (String k : iterable) {
            if (StringUtils.isNotBlank(keyPrefix) && k.startsWith(keyPrefix)) {
                result.add(k.substring(keyPrefix.length()));
            } else {
                result.add(k);
            }
        }
        return result;
    }
}
