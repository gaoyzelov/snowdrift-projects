package com.snowdrift.framework.cache;

import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.util.AssertUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * 缓存服务抽象基类
 * <p>
 * 提供序列化、key 前缀、TTL 默认值等公共逻辑。
 * 序列化统一委托给 {@link ICacheSerializer}，确保 Redis / Caffeine 数据格式一致。
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
    protected final ICacheSerializer serializer;

    protected AbstractCacheService(SnowdriftCacheProperties properties, ICacheSerializer serializer) {
        AssertUtil.notNull(properties, "缓存属性配置不能为空");
        AssertUtil.notNull(serializer, "缓存序列化器不能为空");
        this.serializer = serializer;
        this.properties = properties;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        String value = doGet(buildKey(key));
        if (value == null) {
            return null;
        }
        return serializer.deserialize(value, type);
    }

    @Override
    public <T> T hget(String key, String hashKey, Class<T> type) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notBlank(hashKey, "缓存哈希键不能为空");
        String value = doHget(buildKey(key), hashKey);
        if (value == null) {
            return null;
        }
        return serializer.deserialize(value, type);
    }

    @Override
    public void put(String key, Object value) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(value, "缓存值不能为空");
        doPut(buildKey(key), serializer.serialize(value));
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(value, "缓存值不能为空");
        Duration effectiveTtl = effectiveTtl(ttl);
        doPut(buildKey(key), serializer.serialize(value), effectiveTtl);
    }

    @Override
    public void hput(String key, String hashKey, Object value) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notBlank(hashKey, "缓存哈希键不能为空");
        AssertUtil.notNull(value, "缓存值不能为空");
        doHput(buildKey(key), hashKey, serializer.serialize(value));
    }

    @Override
    public boolean putIfAbsent(String key, Object value) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(value, "缓存值不能为空");
        return doPutIfAbsent(buildKey(key), serializer.serialize(value));
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(value, "缓存值不能为空");
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
    public void hdelete(String key, String hashKey) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notBlank(hashKey, "缓存哈希键不能为空");
        doHdelete(buildKey(key), hashKey);
    }

    @Override
    public long batchDelete(Collection<String> keys) {
        AssertUtil.notEmpty(keys, "缓存键集合不能为空");
        List<String> keyList = keys.stream().map(this::buildKey).toList();
        return doBatchDelete(keyList);
    }

    @Override
    public boolean exists(String key) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        return doExists(buildKey(key));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(ttl, "缓存TTL不能为空");
        return doExpire(buildKey(key), ttl);
    }

    @Override
    public long getExpire(String key) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        return doGetExpire(buildKey(key));
    }

    @Override
    public long increment(String key, Duration ttl) {
        AssertUtil.notBlank(key, "缓存键不能为空");
        AssertUtil.notNull(ttl, "缓存TTL不能为空");
        return doIncrement(buildKey(key), ttl);
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
    protected abstract String doHget(String key, String hashKey);
    protected abstract void doPut(String key, String value);
    protected abstract void doHput(String key, String hashKey, String value);
    protected abstract void doPut(String key, String value, Duration ttl);
    protected abstract boolean doPutIfAbsent(String key, String value);
    protected abstract boolean doPutIfAbsent(String key, String value, Duration ttl);
    protected abstract boolean doDelete(String keys);
    protected abstract boolean doHdelete(String key, String hashKey);
    protected abstract long doBatchDelete(List<String> keys);
    protected abstract boolean doExists(String key);
    protected abstract boolean doExpire(String key, Duration ttl);
    protected abstract long doGetExpire(String key);
    protected abstract long doIncrement(String key, Duration ttl);
}
