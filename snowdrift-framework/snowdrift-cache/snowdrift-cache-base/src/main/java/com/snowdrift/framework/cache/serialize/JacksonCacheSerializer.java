package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
public class JacksonCacheSerializer extends AbstractCacheSerializer {

    private final ObjectMapper objectMapper;

    public JacksonCacheSerializer() {
        this.objectMapper = createObjectMapper();
    }

    @Override
    protected String doSerialize(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Override
    protected <T> T doDeserialize(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(new JavaTimeModule());
        return om;
    }
}
