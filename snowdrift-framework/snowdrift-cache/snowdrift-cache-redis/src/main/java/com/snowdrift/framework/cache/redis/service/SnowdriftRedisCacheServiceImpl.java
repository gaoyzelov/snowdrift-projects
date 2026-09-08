package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存实现
 * <p>
 * 基于 {@link RedisTemplate}{@code <String, String>}，统一使用 JSON 字符串存储。
 * 序列化由 {@link ICacheSerializer} 统一处理，与 Caffeine / Redisson 后端数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public class SnowdriftRedisCacheServiceImpl extends AbstractCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 原子自增并刷新过期时间脚本：INCR 与 EXPIRE 在 Redis 端原子执行，
     * 避免两步分离导致"自增成功但过期设置失败"产生永不过期的计数键
     */
    private static final RedisScript<Long> INCR_WITH_TTL_SCRIPT = new DefaultRedisScript<>("""
            local v = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            return v
            """, Long.class);

    public SnowdriftRedisCacheServiceImpl(SnowdriftCacheProperties properties,
                                          ICacheSerializer serializer,
                                          RedisTemplate<String, String> redisTemplate) {
        super(properties, serializer);
        this.redisTemplate = redisTemplate;
    }

    // =================== AbstractCacheService 抽象方法实现 ===================

    @Override
    public String doGet(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    protected String doHget(String key, String hashKey) {
        return (String) redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public void doPut(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    protected void doHput(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public void doPut(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean doDelete(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    protected boolean doHdelete(String key, String hashKey) {
        return redisTemplate.opsForHash().delete(key, hashKey) > 0;
    }

    @Override
    public long doBatchDelete(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    public boolean doExists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public boolean doExpire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    @Override
    public long doGetExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    protected long doIncrement(String key, Duration ttl) {
        // EXPIRE 粒度为秒，按毫秒向上取整（至少 1 秒），避免小数秒截断导致滚动窗口被缩短
        long expire = Math.max(1, (ttl.toMillis() + 999) / 1000);
        return redisTemplate.execute(INCR_WITH_TTL_SCRIPT, List.of(key), String.valueOf(expire));
    }
}
