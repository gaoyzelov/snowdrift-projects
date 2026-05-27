package com.snowdrift.framework.security.spring.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.common.util.DesensitizeUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于内存的 {@link TokenStore} 默认实现
 * <p>
 * 使用 Caffeine Cache 存储 Token 映射，自动过期 + 最大容量保护。
 * 生产环境建议启用 Redis（引入 {@code spring-boot-starter-data-redis} 即可自动切换）。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class InMemoryTokenStore implements TokenStore {

    private static final int MAX_SIZE = 10000;

    private final long defaultTimeoutSeconds;
    private final Cache<String, SecurityContext> cache;

    public InMemoryTokenStore(long defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(defaultTimeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void put(String token, SecurityContext context, long timeout) {
        long ttl = timeout > 0 ? timeout : defaultTimeoutSeconds;
        cache.policy().expireVariably()
                .ifPresent(p -> p.put(token, context, ttl, TimeUnit.SECONDS));
        log.trace("内存 TokenStore 写入: token={}, expireIn={}s", mask(token), ttl);
    }

    @Override
    public SecurityContext get(String token) {
        return cache.getIfPresent(token);
    }

    @Override
    public void remove(String token) {
        cache.invalidate(token);
        log.trace("内存 TokenStore 移除: token={}", mask(token));
    }

    private String mask(String token) {
        return DesensitizeUtil.process(token, "(?<=.{8}).+", "***");
    }
}
