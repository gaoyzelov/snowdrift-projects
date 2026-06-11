package com.snowdrift.framework.cache;

import com.snowdrift.framework.common.constant.StrConst;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * 缓存服务抽象基类
 * <p>
 * 提供 Jackson 序列化、key 前缀、TTL 默认值等公共逻辑，
 * 通过 {@link CacheSerializer} 确保 Redis / Caffeine 数据格式一致。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
public abstract class AbstractCacheService implements ICacheService {

    /**
     * 原始 key 前缀，子类构造器中赋值
     */
    protected String keyPrefix = StrConst.EMPTY;

    /**
     * 含分隔符的完整前缀（如 "cache:"），由 {@link #resolveKeyPrefix()} 计算
     */
    protected String resolvedPrefix = StrConst.EMPTY;

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
     * 拼接 key 前缀（使用 resolvedPrefix，含分隔符）
     */
    protected String buildKey(String key) {
        return resolvedPrefix + key;
    }

    /**
     * 根据 keyPrefix 计算含分隔符的完整前缀，子类构造器中调用
     */
    protected void resolveKeyPrefix() {
        if (StringUtils.isBlank(keyPrefix)) {
            this.resolvedPrefix = StrConst.EMPTY;
        } else {
            this.resolvedPrefix = keyPrefix.endsWith(StrConst.COLON)
                    ? keyPrefix
                    : keyPrefix + StrConst.COLON;
        }
    }

    /**
     * TTL 为 null 时取默认值
     */
    protected Duration effectiveTtl(Duration ttl) {
        return ttl != null ? ttl : defaultTtl;
    }
}
