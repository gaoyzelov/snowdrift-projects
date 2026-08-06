package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.snowdrift.framework.common.exception.BizException;

/**
 * 基于 Jackson 的缓存序列化器
 * <p>
 * 安全设计：
 * <ul>
 *   <li>不启用 {@code DefaultTyping}，不向 JSON 写入 {@code @class} 类型元数据</li>
 *   <li>反序列化依赖调用方传入的 {@link Class} 参数，而非 JSON 内嵌类型</li>
 *   <li>忽略未知字段（兼容旧版本缓存数据中可能存在的 {@code @class} 字段）</li>
 * </ul>
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
public class JacksonCacheSerializer implements CacheSerializer {

    private final ObjectMapper objectMapper;

    public JacksonCacheSerializer() {
        this.objectMapper = createObjectMapper();
    }

    @Override
    public String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
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
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BizException("cache.deserialize.failed", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        // 序列化所有字段（包括 private）
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 不启用 DefaultTyping —— 类型信息由调用方 API 参数提供，消除反序列化注入风险
        // 反序列化忽略未知字段，兼容旧缓存数据中可能存在的 @class 等类型元数据
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // LocalDateTime 不使用 timestamp 格式
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 支持 Java 8 日期时间类型
        om.registerModule(new JavaTimeModule());
        return om;
    }
}
