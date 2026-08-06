package com.snowdrift.framework.cache.serialize;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.snowdrift.framework.common.exception.BizException;

/**
 * 基于 Fastjson2 的缓存序列化器
 * <p>
 * 安全设计：
 * <ul>
 *   <li>不写入 {@code @type} 类型元数据（关闭 {@link JSONWriter.Feature#WriteClassName}）</li>
 *   <li>反序列化依赖调用方传入的 {@link Class} 参数，而非 JSON 内嵌类型</li>
 *   <li>忽略未知字段，兼容不同版本间的数据结构差异</li>
 * </ul>
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
public class FastJson2CacheSerializer implements CacheSerializer {

    @Override
    public String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.toJSONString(value, JSONWriter.Feature.FieldBased);
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
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            throw new BizException("cache.deserialize.failed", e);
        }
    }
}
