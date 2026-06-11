package com.snowdrift.framework.cache.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Caffeine 本地缓存实现
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
public class CaffeineCacheServiceImpl extends AbstractCacheService {

    private final Cache<String, Object> cache;

    /**
     * 每 key 的过期时间戳（ms），用于支持 per-key TTL，
     * Caffeine 本身只支持全局 TTL，不支持 per-key TTL
     */
    private final Map<String, Long> expireMap = new ConcurrentHashMap<>();

    /** 通配符 Pattern 缓存，避免每次编译正则 */
    private final Map<String, java.util.regex.Pattern> patternCache = new ConcurrentHashMap<>();

    public CaffeineCacheServiceImpl(CacheProperties properties) {
        AssertUtil.notNull(properties, "cache.config.required");
        CacheProperties.CaffeineConfig config = properties.getCaffeine();

        this.keyPrefix = Objects.toString(properties.getKeyPrefix(), StrConst.EMPTY);
        this.defaultTtl = properties.getDefaultTtl() != null ? properties.getDefaultTtl() : config.getTtl();
        resolveKeyPrefix();

        this.cache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .removalListener((key, value, cause) -> {
                    if (key != null) {
                        expireMap.remove(key);
                    }
                })
                .build();
    }

    // =================== ICacheService 实现 ===================

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "cache.key.required");
        String realKey = buildKey(key);
        // 检查 per-key 过期
        checkExpired(realKey);
        Object value = cache.getIfPresent(realKey);
        if (value == null) {
            return null;
        }
        return deserialize(value.toString(), type);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        String realKey = buildKey(key);
        Duration effectiveTtl = effectiveTtl(ttl);
        String json = serialize(value);
        cache.put(realKey, json);
        if (effectiveTtl != null) {
            expireMap.put(realKey, System.currentTimeMillis() + effectiveTtl.toMillis());
        }
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        String realKey = buildKey(key);
        checkExpired(realKey);
        String json = serialize(value);
        // 使用 ConcurrentMap 原子 putIfAbsent，替代 synchronized 双重检查
        Object existing = cache.asMap().putIfAbsent(realKey, json);
        if (existing != null) {
            return false;
        }
        Duration et = effectiveTtl(ttl);
        if (et != null) {
            expireMap.put(realKey, System.currentTimeMillis() + et.toMillis());
        }
        return true;
    }

    @Override
    public boolean delete(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        String realKey = buildKey(key);
        expireMap.remove(realKey);
        boolean existed = cache.getIfPresent(realKey) != null;
        cache.invalidate(realKey);
        return existed;
    }

    @Override
    public long delete(Collection<String> keys) {
        AssertUtil.notNull(keys, "cache.keys.required");
        long count = 0;
        for (String key : keys) {
            if (delete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean exists(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return get(key, Object.class) != null;
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(ttl, "cache.ttl.required");
        String realKey = buildKey(key);
        if (cache.getIfPresent(realKey) == null) {
            return false;
        }
        expireMap.put(realKey, System.currentTimeMillis() + ttl.toMillis());
        return true;
    }

    @Override
    public long getExpire(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        String realKey = buildKey(key);
        if (cache.getIfPresent(realKey) == null) {
            return -2;
        }
        Long expireAt = expireMap.get(realKey);
        if (expireAt == null) {
            return -1;
        }
        long remaining = expireAt - System.currentTimeMillis();
        return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : -2;
    }

    @Override
    public Set<String> keys(String pattern) {
        AssertUtil.notBlank(pattern, "cache.pattern.required");
        // 去掉前缀后做简单通配符匹配
        String matchPattern = pattern;
        if (StringUtils.isNotBlank(resolvedPrefix) && matchPattern.startsWith(resolvedPrefix)) {
            matchPattern = matchPattern.substring(resolvedPrefix.length());
        }
        final String finalPattern = matchPattern;
        return cache.asMap().keySet().stream()
                .map(k -> {
                    if (StringUtils.isNotBlank(resolvedPrefix) && k.startsWith(resolvedPrefix)) {
                        return k.substring(resolvedPrefix.length());
                    }
                    return k;
                })
                .filter(k -> matchWildcard(k, finalPattern))
                .collect(Collectors.toSet());
    }

    /**
     * 检查 per-key 是否过期，过期则清理
     */
    private void checkExpired(String realKey) {
        Long expireAt = expireMap.get(realKey);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            cache.invalidate(realKey);
            expireMap.remove(realKey);
        }
    }

    /**
     * 简单通配符匹配（支持 * 和 ?）
     */
    private boolean matchWildcard(String str, String pattern) {
        java.util.regex.Pattern p = patternCache.computeIfAbsent(pattern, ptn -> {
            String regex = ptn
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
            return java.util.regex.Pattern.compile(regex);
        });
        return p.matcher(str).matches();
    }
}
