package com.snowdrift.framework.cache.redisson.service;

import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.config.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.List;

/**
 * Redisson 缓存实现
 * <p>
 * 基于 {@link RedissonClient}，统一使用 JSON 字符串（{@link StringCodec}）存储。
 * 序列化由 {@link CacheSerializer} 统一处理，与 Caffeine / Redis 后端数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public class SnowdriftRedissonCacheServiceImpl extends AbstractCacheService {

    private final RedissonClient redissonClient;

    public SnowdriftRedissonCacheServiceImpl(SnowdriftCacheProperties properties,
                                             CacheSerializer serializer,
                                             RedissonClient redissonClient) {
        super(properties, serializer);
        this.redissonClient = redissonClient;
    }

    // =================== AbstractCacheService 抽象方法实现 ===================

    @Override
    public String doGet(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    @Override
    public void doPut(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value);
    }

    @Override
    public void doPut(String key, String value, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value, ttl);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.setIfAbsent(value);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.setIfAbsent(value,ttl);
    }

    @Override
    public boolean doDelete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public long doBatchDelete(List<String> keys) {
        return redissonClient.getKeys().delete(keys.toArray(String[]::new));
    }

    @Override
    public boolean doExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public boolean doExpire(String key, Duration ttl) {
        return redissonClient.getBucket(key).expire(ttl);
    }

    @Override
    public long doGetExpire(String key) {
        long remain = redissonClient.getBucket(key).remainTimeToLive();
        return remain > 0 ? Duration.ofMillis(remain).toSeconds() : remain;
    }
}
