package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.common.constant.StrConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

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
    private final String keyPrefix;

    public RedisTokenStore(RedisTemplate<String, Object> redisTemplate,
                           long timeout, long idle,
                           String keyPrefix) {
        super(Duration.ofSeconds(timeout), Duration.ofSeconds(idle));
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    protected void doPut(String token, TokenEntry entry, Duration ttl) {
        redisTemplate.opsForValue().set(buildKey(token), entry, ttl);
    }

    @Override
    protected TokenEntry doGet(String token) {
        Object value = redisTemplate.opsForValue().get(buildKey(token));
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
        Duration remain = Duration.ofMillis(entry.getExpireAt() - now);
        Duration ttl = idle.isNegative() ? remain : remain.compareTo(idle) < 0 ? remain : idle;
        if (!ttl.isNegative()) {
            redisTemplate.opsForValue().set(buildKey(token), updated, ttl);
        }
    }

    @Override
    public void remove(String token) {
        redisTemplate.delete(buildKey(token));
        log.trace("Redis TokenStore 移除: token={}", token);
    }

    private String buildKey(String token) {
        return keyPrefix + StrConst.COLON + token;
    }
}
