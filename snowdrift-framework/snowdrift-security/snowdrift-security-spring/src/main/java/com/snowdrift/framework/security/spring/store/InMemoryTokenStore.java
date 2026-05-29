package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.store.TokenStore;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 {@link TokenStore} 默认实现
 * <p>
 * 使用 ConcurrentHashMap 存储 Token 映射，get 时惰性检查过期。
 * 生产环境建议启用 Redis（引入 {@code spring-boot-starter-data-redis} 即可自动切换）。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class InMemoryTokenStore implements TokenStore {

    private final long defaultTimeoutSeconds;
    private final ConcurrentHashMap<String, TokenEntry> map = new ConcurrentHashMap<>();

    public InMemoryTokenStore(long defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    @Override
    public void put(String token, SecurityContext context, long timeout) {
        long ttl = timeout > 0 ? timeout : defaultTimeoutSeconds;
        long expireAt = System.currentTimeMillis() + ttl * 1000;
        map.put(token, new TokenEntry(context, expireAt));
        log.trace("内存 TokenStore 写入: token={}, expireIn={}s", token, ttl);
    }

    @Override
    public SecurityContext get(String token) {
        TokenEntry entry = map.get(token);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            map.remove(token);
            return null;
        }
        return entry.context;
    }

    @Override
    public void remove(String token) {
        map.remove(token);
        log.trace("内存 TokenStore 移除: token={}", token);
    }

    private record TokenEntry(SecurityContext context, long expireAt) {
    }
}
