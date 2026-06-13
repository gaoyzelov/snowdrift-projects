package com.snowdrift.framework.cache.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Caffeine 本地缓存实现
 * <p>
 * 完全依赖 Caffeine 原生 {@code expireAfterWrite} 过期机制，
 * 不支持 per-key TTL（{@link #expire} 和 {@link #getExpire} 返回语义降级值）。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
public class CaffeineCacheServiceImpl extends AbstractCacheService {

    private final Cache<String, Object> cache;

    /** 通配符 Pattern 缓存，避免每次编译正则 */
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public CaffeineCacheServiceImpl(CacheProperties properties) {
        AssertUtil.notNull(properties, "cache.config.required");
        setKeyPrefix(properties.getKeyPrefix());
        setDefaultTtl(properties.getKeyTtl());

        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getKeyTtl())
                .build();
    }

    // =================== ICacheService 实现 ===================

    @Override
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "cache.key.required");
        Object value = cache.getIfPresent(buildKey(key));
        if (value == null) {
            return null;
        }
        return deserialize(value.toString(), type);
    }

    @Override
    public void put(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        cache.put(buildKey(key), serialize(value));
    }

    /**
     * Caffeine 不支持 per-key TTL
     */
    @Override
    public void put(String key, Object value, Duration ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean putIfAbsent(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        String realKey = buildKey(key);
        String json = serialize(value);
        Object existing = cache.asMap().putIfAbsent(realKey, json);
        return existing == null;
    }

    /**
     * Caffeine 不支持 per-key TTL
     */
    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        String realKey = buildKey(key);
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
        return cache.getIfPresent(buildKey(key)) != null;
    }

    /**
     * Caffeine 原生不支持 per-key TTL
     */
    @Override
    public boolean expire(String key, Duration ttl) {
        throw new UnsupportedOperationException();
    }

    /**
     * Caffeine 原生不支持查询剩余 TTL
     *
     */
    @Override
    public long getExpire(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keys(String pattern) {
        AssertUtil.notBlank(pattern, "cache.pattern.required");
        // 将用户 pattern 补上前缀，与内部存储的完整 key 对齐，只在最后输出时去掉前缀
        String realPattern = StringUtils.isNotBlank(keyPrefix) && !pattern.startsWith(keyPrefix)
                ? keyPrefix + pattern
                : pattern;
        return cache.asMap().keySet().stream()
                .filter(k -> matchWildcard(k, realPattern))
                .map(k -> {
                    if (StringUtils.isNotBlank(keyPrefix) && k.startsWith(keyPrefix)) {
                        return k.substring(keyPrefix.length());
                    }
                    return k;
                })
                .collect(Collectors.toSet());
    }

    /**
     * 简单通配符匹配（支持 * 和 ?）
     */
    private boolean matchWildcard(String str, String pattern) {
        Pattern p = patternCache.computeIfAbsent(pattern, ptn -> {
            String regex = ptn
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
            return Pattern.compile(regex);
        });
        return p.matcher(str).matches();
    }
}
