package com.snowdrift.framework.cache.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.common.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
public class CaffeineCacheServiceImpl extends AbstractCacheService {

    private final Cache<String, Object> cache;

    /** 通配符 Pattern 缓存，有界 + TTL 防止内存泄漏 */
    private final Cache<String, Pattern> patternCache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public CaffeineCacheServiceImpl(CacheProperties properties, CacheSerializer serializer) {
        super(serializer);
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
     * Caffeine 不支持 per-key TTL，静默降级为全局默认 TTL
     */
    @Override
    public void put(String key, Object value, Duration ttl) {
        log.warn("Caffeine 不支持 per-key TTL，已降级为全局默认 TTL: key={}", key);
        put(key, value);
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
     * Caffeine 不支持 per-key TTL，静默降级为全局默认 TTL
     */
    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        log.warn("Caffeine 不支持 per-key TTL，已降级为全局默认 TTL: key={}", key);
        return putIfAbsent(key, value);
    }

    @Override
    public boolean delete(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return cache.asMap().remove(buildKey(key)) != null;
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
     * Caffeine 原生不支持 per-key TTL 动态修改，返回 false 表示未生效
     */
    @Override
    public boolean expire(String key, Duration ttl) {
        log.warn("Caffeine 不支持 per-key TTL 修改，操作已忽略: key={}", key);
        return false;
    }

    /**
     * Caffeine 原生不支持查询剩余 TTL，返回 -2 表示不支持该操作
     */
    @Override
    public long getExpire(String key) {
        log.debug("Caffeine 不支持查询 per-key TTL: key={}", key);
        return -2;
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
     * 简单通配符匹配（支持 * 和 ?），其他正则特殊字符全部转义
     */
    private boolean matchWildcard(String str, String pattern) {
        Pattern p = patternCache.get(pattern, ptn -> {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < ptn.length(); i++) {
                char c = ptn.charAt(i);
                switch (c) {
                    case '*': regex.append(".*"); break;
                    case '?': regex.append('.'); break;
                    // 正则元字符全部转义
                    case '.': case '(': case ')': case '[': case ']':
                    case '+': case '^': case '$': case '|': case '\\':
                    case '{': case '}':
                        regex.append('\\').append(c);
                        break;
                    default:
                        regex.append(c);
                }
            }
            return Pattern.compile(regex.toString());
        });
        return p.matcher(str).matches();
    }
}
