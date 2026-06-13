package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的 {@link TokenStore} 默认实现
 * <p>
 * 使用 ConcurrentHashMap 存储 Token 映射，get 时惰性检查过期，
 * 后台定时任务兜底清理已过期的条目。
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
    private final long idleTimeoutSeconds;
    private final ConcurrentHashMap<String, TokenEntry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-store-cleaner");
        t.setDaemon(true);
        return t;
    });

    public InMemoryTokenStore(long defaultTimeoutSeconds, long idleTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        cleaner.scheduleWithFixedDelay(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        cleaner.shutdown();
    }

    @Override
    public void put(String token, SecurityContext context, long timeout) {
        long ttl = timeout > 0 ? timeout : defaultTimeoutSeconds;
        long now = System.currentTimeMillis();
        long expireAt = now + ttl * 1000;
        map.put(token, new TokenEntry(context, expireAt, now));
        log.trace("内存 TokenStore 写入: token={}, expireIn={}s", token, ttl);
    }

    @Override
    public SecurityContext get(String token) {
        TokenEntry entry = map.get(token);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (entry.expireAt < now) {
            map.remove(token);
            return null;
        }
        // 刷新最后活跃时间（ConcurrentHashMap 非原子替换，但允许少量不一致）
        map.put(token, new TokenEntry(entry.context, entry.expireAt, now));
        return entry.context;
    }

    @Override
    public void remove(String token) {
        map.remove(token);
        log.trace("内存 TokenStore 移除: token={}", token);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        long idleThreshold = idleTimeoutSeconds * 1000;
        map.forEach((token, entry) -> {
            if (entry.expireAt < now || (now - entry.lastActiveAt) > idleThreshold) {
                map.remove(token);
                log.trace("定时清理 Token: token={}, reason={}", token,
                        entry.expireAt < now ? "expired" : "idle");
            }
        });
    }

    private record TokenEntry(SecurityContext context, long expireAt, long lastActiveAt) {
    }
}
