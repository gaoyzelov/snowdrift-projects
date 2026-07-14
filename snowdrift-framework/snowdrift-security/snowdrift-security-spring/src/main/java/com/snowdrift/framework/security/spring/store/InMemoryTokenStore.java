package com.snowdrift.framework.security.spring.store;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 基于内存的 {@link TokenStore} 默认实现
 * <p>
 * 使用 Guava Cache 存储 Token 映射，LRU 自动淘汰防止内存溢出。
 * 绝对过期和闲置过期由 {@link AbstractTokenStore#get(String)} 统一校验。
 * 生产环境建议启用 Redis（引入 {@code spring-boot-starter-data-redis} 即可自动切换）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class InMemoryTokenStore extends AbstractTokenStore {

    private final Cache<String, TokenEntry> cache;

    public InMemoryTokenStore(long defaultTimeoutSeconds, long idleTimeoutSeconds) {
        super(defaultTimeoutSeconds, idleTimeoutSeconds);
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(2048)
                .expireAfterAccess(Duration.ofSeconds(Math.max(defaultTimeoutSeconds, idleTimeoutSeconds)))
                .build();
    }

    @Override
    protected void doPut(String token, TokenEntry entry, long ttl) {
        cache.put(token, entry);
    }

    @Override
    protected TokenEntry doGet(String token) {
        return cache.getIfPresent(token);
    }

    @Override
    protected void touch(String token, TokenEntry entry) {
        cache.asMap().computeIfPresent(token, (k, v) ->
            new TokenEntry(v.getContext(), v.getExpireAt(), System.currentTimeMillis()));
    }

    @Override
    public void remove(String token) {
        cache.invalidate(token);
        log.trace("内存 TokenStore 移除: token={}", token);
    }
}
