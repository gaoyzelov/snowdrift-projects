package com.snowdrift.framework.cache.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Caffeine 本地缓存实现
 * <p>
 * 完全依赖 Caffeine 原生 {@code expireAfterWrite} 过期机制，
 * 不支持 per-key TTL（{@link #expire} 和 {@link #getExpire} 返回语义降级值）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftCaffeineCacheServiceImpl extends AbstractCacheService {

    private final Cache<String, String> cache;

    public SnowdriftCaffeineCacheServiceImpl(SnowdriftCacheProperties properties, ICacheSerializer serializer) {
        super(properties, serializer);
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getKeyTtl())
                .build();
    }

    // =================== AbstractCacheService 抽象方法实现 ===================

    @Override
    public String doGet(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    protected String doHget(String key, String hashKey) {
        throw new UnsupportedOperationException("Caffeine 不支持hget 操作");
    }

    @Override
    public void doPut(String key, String value) {
        cache.put(key, value);
    }

    @Override
    protected void doHput(String key, String hashKey, String value) {
        throw new UnsupportedOperationException("Caffeine 不支持hput 操作");
    }

    @Override
    public void doPut(String key, String value, Duration ttl) {
        throw new UnsupportedOperationException("Caffeine 不支持 per-key TTL");
    }

    @Override
    public boolean doPutIfAbsent(String key, String value) {
        Object existing = cache.asMap().putIfAbsent(key, value);
        return existing == null;
    }

    @Override
    public boolean doPutIfAbsent(String key, String value, Duration ttl) {
        throw new UnsupportedOperationException("Caffeine 不支持 per-key TTL");
    }

    @Override
    public boolean doDelete(String key) {
        return cache.asMap().remove(key) != null;
    }

    @Override
    protected boolean doHdelete(String key, String hashKey) {
        throw new UnsupportedOperationException("Caffeine 不支持hdelete 操作");
    }

    @Override
    public long doBatchDelete(List<String> keys) {
        long count = 0;
        for (String key : keys) {
            if (doDelete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean doExists(String key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * Caffeine 原生不支持 per-key TTL 动态修改，返回 false 表示未生效
     */
    @Override
    public boolean doExpire(String key, Duration ttl) {
        throw new UnsupportedOperationException("Caffeine 不支持 per-key TTL");
    }

    /**
     * Caffeine 原生不支持查询剩余 TTL，返回 -2 表示不支持该操作
     */
    @Override
    public long doGetExpire(String key) {
        throw new UnsupportedOperationException("Caffeine 不支持查询 per-key TTL");
    }

    @Override
    protected long doIncrement(String key, Duration ttl) {
        throw new UnsupportedOperationException("Caffeine 不支持 increment 操作");
    }
}
