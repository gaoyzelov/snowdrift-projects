package com.snowdrift.framework.security.spring.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 {@link TokenStore} 实现
 * <p>
 * 使用单个 Redis key 存储 {@link TokenEntry}（JSON），
 * Redis TTL 天然管理闲置过期，{@code expireAt} 字段管理绝对过期。
 * 多实例部署时 Token 自动共享，无需额外的会话同步。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/14
 * @since 1.0.0
 */
@Slf4j
public class RedisTokenStore extends AbstractTokenStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String tokenKeyPrefix;

    public RedisTokenStore(RedisTemplate<String, Object> redisTemplate,
                           long defaultTimeoutSeconds, long idleTimeoutSeconds,
                           String tokenKeyPrefix) {
        super(defaultTimeoutSeconds, idleTimeoutSeconds);
        this.redisTemplate = redisTemplate;
        this.tokenKeyPrefix = tokenKeyPrefix;
    }

    @Override
    protected void doPut(String token, TokenEntry entry, long ttl) {
        // Redis TTL 取闲置超时与绝对超时的较小值；闲置超时为 0 时不限制
        long effectiveTtl = idleTimeoutSeconds > 0 ? Math.min(ttl, idleTimeoutSeconds) : ttl;
        redisTemplate.opsForValue().set(tokenKey(token), entry, effectiveTtl, TimeUnit.SECONDS);
    }

    @Override
    protected TokenEntry doGet(String token) {
        Object value = redisTemplate.opsForValue().get(tokenKey(token));
        if (value instanceof TokenEntry e) {
            return e;
        }
        return null;
    }

    @Override
    protected void touch(String token, TokenEntry entry) {
        // 刷新 lastActiveAt 并写回 Redis，使闲置超时基于真实最后活跃时间计算
        long now = System.currentTimeMillis();
        TokenEntry updated = new TokenEntry(entry.getContext(), entry.getExpireAt(), now);
        long absoluteRemain = (entry.getExpireAt() - now) / 1000;
        long effectiveTtl = idleTimeoutSeconds > 0
            ? Math.min(absoluteRemain, idleTimeoutSeconds)
            : absoluteRemain;
        if (effectiveTtl > 0) {
            redisTemplate.opsForValue().set(tokenKey(token), updated, effectiveTtl, TimeUnit.SECONDS);
        }
    }

    @Override
    public void remove(String token) {
        redisTemplate.delete(tokenKey(token));
        log.trace("Redis TokenStore 移除: token={}", token);
    }

    private String tokenKey(String token) {
        return tokenKeyPrefix + token;
    }
}
