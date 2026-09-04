package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
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
        this.objectMapper = defaultMapper();
    }

    @Override
    protected String doSerialize(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Override
    protected <T> T doDeserialize(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }

    public static ObjectMapper defaultMapper() {
        ObjectMapper om = new ObjectMapper();
        // 所有访问权限字段均可序列化（private也可以）
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 反序列化遇到实体不存在字段不抛异常
        om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 关闭日期输出时间戳，输出ISO字符串
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 注册JavaTimeModule，支持LocalDateTime/LocalDate等java.time类
        om.registerModule(new JavaTimeModule());
        // 开启类型写入，解决Redis反序列化变成LinkedHashMap问题
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return om;
    }
}
