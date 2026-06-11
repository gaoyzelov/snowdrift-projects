package com.snowdrift.framework.cache.redisson.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        this.keyPrefix = Objects.toString(properties.getKeyPrefix(), StrConst.EMPTY);
        this.defaultTtl = properties.getDefaultTtl();
        resolveKeyPrefix();
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
            bucket.set(value, effectiveTtl.toMillis(), TimeUnit.MILLISECONDS);
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
            return bucket.trySet(value, effectiveTtl.toMillis(), TimeUnit.MILLISECONDS);
        }
        return bucket.trySet(value);
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
        return remain > 0 ? TimeUnit.MILLISECONDS.toSeconds(remain) : remain;
    }

    @Override
    public Set<String> keys(String pattern) {
        AssertUtil.notBlank(pattern, "cache.pattern.required");
        RKeys rKeys = redissonClient.getKeys();
        Iterable<String> iterable = rKeys.getKeysByPattern(buildKey(pattern));
        Set<String> result = new java.util.HashSet<>();
        for (String k : iterable) {
            if (StringUtils.isNotBlank(resolvedPrefix) && k.startsWith(resolvedPrefix)) {
                result.add(k.substring(resolvedPrefix.length()));
            } else {
                result.add(k);
            }
        }
        return result;
    }
}
