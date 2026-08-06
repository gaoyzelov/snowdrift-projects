package com.snowdrift.framework.cache;

import com.snowdrift.framework.cache.config.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.AssertUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * 缓存服务抽象基类
 * <p>
 * 提供序列化、key 前缀、TTL 默认值等公共逻辑。
 * 序列化统一委托给 {@link CacheSerializer}，确保 Redis / Caffeine 数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public abstract class AbstractCacheService implements ICacheService {
    /**
     * 缓存配置属性
     */
    protected final SnowdriftCacheProperties properties;

    /**
     * 序列化器
     */
    protected final CacheSerializer serializer;

    protected AbstractCacheService(SnowdriftCacheProperties properties, CacheSerializer serializer) {
        AssertUtil.notNull(properties, "cache.config.required");
        AssertUtil.notNull(serializer, "cache.serializer.required");
        this.serializer = serializer;
        this.properties = properties;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "cache.key.required");
        String value = doGet(buildKey(key));
        if (value == null) {
            return null;
        }
        return serializer.deserialize(value, type);
    }

    @Override
    public void put(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        doPut(buildKey(key), serializer.serialize(value));
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        doPut(buildKey(key), serializer.serialize(value), ttl);
    }

    @Override
    public boolean putIfAbsent(String key, Object value) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        return doPutIfAbsent(buildKey(key), serializer.serialize(value));
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(value, "cache.value.required");
        Duration effectiveTtl = effectiveTtl(ttl);
        if (effectiveTtl != null) {
            return doPutIfAbsent(buildKey(key), serializer.serialize(value), effectiveTtl);
        } else {
            return doPutIfAbsent(buildKey(key), serializer.serialize(value));
        }
    }

    @Override
    public boolean delete(String key) {
        return doDelete(buildKey(key));
    }

    @Override
    public long batchDelete(Collection<String> keys) {
        AssertUtil.notEmpty(keys, "cache.keys.required");
        List<String> keyList = keys.stream().map(this::buildKey).toList();
        return doBatchDelete(keyList);
    }

    @Override
    public boolean exists(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return doExists(buildKey(key));
    }



    @Override
    public boolean expire(String key, Duration ttl) {
        AssertUtil.notBlank(key, "cache.key.required");
        AssertUtil.notNull(ttl, "cache.ttl.required");
        return doExpire(buildKey(key), ttl);
    }

    @Override
    public long getExpire(String key) {
        AssertUtil.notBlank(key, "cache.key.required");
        return doGetExpire(buildKey(key));
    }

    /**
     * 拼接 key 前缀
     */
    protected String buildKey(String key) {
        return properties.getKeyPrefix() + StrConst.COLON + key;
    }

    /**
     * TTL 为 null 时取默认值
     */
    protected Duration effectiveTtl(Duration ttl) {
        return ttl != null ? ttl : properties.getKeyTtl();
    }

    protected abstract String doGet(String key);
    protected abstract void doPut(String key, String value);
    protected abstract void doPut(String key, String value, Duration ttl);
    protected abstract boolean doPutIfAbsent(String key, String value);
    protected abstract boolean doPutIfAbsent(String key, String value, Duration ttl);
    protected abstract boolean doDelete(String keys);
    protected abstract long doBatchDelete(List<String> keys);
    protected abstract boolean doExists(String key);
    protected abstract boolean doExpire(String key, Duration ttl);
    protected abstract long doGetExpire(String key);
}
