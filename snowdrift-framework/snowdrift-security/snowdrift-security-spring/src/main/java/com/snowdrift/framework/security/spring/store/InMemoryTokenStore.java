package com.snowdrift.framework.security.spring.store;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的 {@link TokenStore} 默认实现
 * <p>
 * 使用 ConcurrentHashMap 存储 Token 映射，后台定时任务兜底清理过期条目。
 * 生产环境建议启用 Redis（引入 {@code spring-boot-starter-data-redis} 即可自动切换）。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class InMemoryTokenStore extends AbstractTokenStore {

    private final ConcurrentHashMap<String, TokenEntry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-store-cleaner");
        t.setDaemon(true);
        return t;
    });

    public InMemoryTokenStore(long defaultTimeoutSeconds, long idleTimeoutSeconds) {
        super(defaultTimeoutSeconds, idleTimeoutSeconds);
        cleaner.scheduleWithFixedDelay(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        cleaner.shutdown();
    }

    @Override
    protected void doPut(String token, TokenEntry entry, long ttl) {
        map.put(token, entry);
    }

    @Override
    protected TokenEntry doGet(String token) {
        return map.get(token);
    }

    @Override
    protected void touch(String token, TokenEntry entry) {
        map.put(token, new TokenEntry(entry.getContext(), entry.getExpireAt(), System.currentTimeMillis()));
    }

    @Override
    public void remove(String token) {
        map.remove(token);
        log.trace("内存 TokenStore 移除: token={}", token);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        long idleThreshold = idleTimeoutSeconds * 1000;
        map.entrySet().removeIf(e -> {
            TokenEntry entry = e.getValue();
            return entry.getExpireAt() < now || (now - entry.getLastActiveAt()) > idleThreshold;
        });
    }
}
