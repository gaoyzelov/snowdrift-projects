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
 * 设计取舍：
 * <ul>
 *   <li>启用 {@code DefaultTyping(NON_FINAL)}，序列化时写入 {@code @class} 类型元数据，
 *       保证接口/父类类型反序列化不会退化为 {@code LinkedHashMap}</li>
 *   <li>因此缓存 JSON 内含全限定类名：实体类重命名/移动包会导致旧缓存读取失败，需清理后重建</li>
 *   <li>忽略未知字段（{@code FAIL_ON_UNKNOWN_PROPERTIES=false}），兼容结构演进</li>
 *   <li>信任前提是缓存数据由本应用自身写入；若缓存存在被外部篡改风险，请勿启用类型写入</li>
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
