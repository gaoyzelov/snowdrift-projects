package com.snowdrift.framework.cache.serialize;

import com.snowdrift.framework.common.exception.BizException;

/**
 * AbstractCacheSerializer
 *
 * @author gaoyzelov
 * @date 2026/8/7-18:00
 * @description 缓存序列化器模板类，统一处理 null-guard 和异常包装
 * @since 1.0.0
 */
public abstract class AbstractCacheSerializer implements CacheSerializer {

    @Override
    public String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return doSerialize(value);
        } catch (Exception e) {
            throw new BizException("cache.serialize.failed", e);
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return doDeserialize(json, type);
        } catch (Exception e) {
            throw new BizException("cache.deserialize.failed", e);
        }
    }

    protected abstract String doSerialize(Object value) throws Exception;

    protected abstract <T> T doDeserialize(String json, Class<T> type) throws Exception;
}
