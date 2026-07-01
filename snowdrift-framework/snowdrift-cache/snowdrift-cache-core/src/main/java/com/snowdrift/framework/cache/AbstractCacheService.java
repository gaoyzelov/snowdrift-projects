package com.snowdrift.framework.cache;

import com.snowdrift.framework.common.constant.StrConst;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Objects;

/**
 * 缓存服务抽象基类
 * <p>
 * 提供 Jackson 序列化、key 前缀、TTL 默认值等公共逻辑，
 * 通过 {@link CacheSerializer} 确保 Redis / Caffeine 数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public abstract class AbstractCacheService implements ICacheService {

    /**
     * key 前缀（已含分隔符 ":"），通过 {@link #setKeyPrefix(String)} 赋值
     */
    protected String keyPrefix = StrConst.EMPTY;

    /**
     * 默认 TTL，null 表示永不过期
     */
    protected Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * 序列化为 JSON 字符串
     */
    protected String serialize(Object value) {
        return CacheSerializer.serialize(value);
    }

    /**
     * 反序列化为目标类型
     */
    protected <T> T deserialize(String json, Class<T> type) {
        return CacheSerializer.deserialize(json, type);
    }

    /**
     * 拼接 key 前缀
     */
    protected String buildKey(String key) {
        return keyPrefix + key;
    }

    /**
     * 设置 key 前缀，自动补充分隔符 ":"
     *
     * @param prefix 原始前缀
     */
    protected void setKeyPrefix(String prefix) {
        if (StringUtils.isNotBlank(prefix)) {
            this.keyPrefix = prefix.endsWith(StrConst.COLON) ? prefix : prefix + StrConst.COLON;
        }
    }

    /**
     * 设置默认 TTL
     *
     * @param defaultTtl 默认过期时间
     */
    protected void setDefaultTtl(Duration defaultTtl) {
        if (Objects.nonNull(defaultTtl)) {
            this.defaultTtl = defaultTtl;
        }
    }

    /**
     * TTL 为 null 时取默认值
     */
    protected Duration effectiveTtl(Duration ttl) {
        return ttl != null ? ttl : defaultTtl;
    }
}
