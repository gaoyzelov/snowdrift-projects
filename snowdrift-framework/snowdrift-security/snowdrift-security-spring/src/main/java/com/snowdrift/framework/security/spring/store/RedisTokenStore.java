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
 * @author 83674
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
        // Redis TTL 设为 min(ttl, idleTimeout)，保证闲置时 key 能被 Redis 自动清除
        long effectiveTtl = Math.min(ttl, idleTimeoutSeconds);
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
        // 刷新 Redis TTL = 刷新闲置窗口
        long effectiveTtl = Math.min(
                (entry.getExpireAt() - System.currentTimeMillis()) / 1000,
                idleTimeoutSeconds);
        if (effectiveTtl > 0) {
            redisTemplate.expire(tokenKey(token), effectiveTtl, TimeUnit.SECONDS);
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
