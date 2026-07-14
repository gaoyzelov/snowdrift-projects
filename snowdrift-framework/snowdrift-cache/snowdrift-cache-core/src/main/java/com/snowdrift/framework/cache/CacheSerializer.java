package com.snowdrift.framework.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.snowdrift.framework.common.exception.BizException;

/**
 * 缓存序列化器
 * <p>
 * 基于 Jackson 的统一序列化实现，所有缓存后端（Redis / Caffeine）共享同一套配置，
 * 确保切换后端时数据格式一致。
 * </p>
 * <p>
 * 特性：
 * <ul>
 *   <li>类型元数据写入 JSON（@class），反序列化时还原具体类型</li>
 *   <li>支持 Java 8 日期时间类型</li>
 *   <li>序列化所有字段（包括 private）</li>
 * </ul>
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public final class CacheSerializer {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    private CacheSerializer() {
    }

    /**
     * 获取共享的 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return INSTANCE;
    }

    /**
     * 序列化为 JSON 字符串
     */
    public static String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return INSTANCE.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("cache.serialize.failed", e);
        }
    }

    /**
     * 反序列化为目标类型
     */
    public static <T> T deserialize(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return INSTANCE.readValue(json, type);
        } catch (Exception e) {
            throw new BizException("cache.deserialize.failed", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        // 序列化所有字段（包括 private）
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 写入类型信息，反序列化时还原具体类型（final 类除外，如 String/Integer）
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        //LocalDateTime类型redis序列化、反序列化异常处理
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 支持 Java 8 日期时间类型
        om.registerModule(new JavaTimeModule());
        om.findAndRegisterModules();
        return om;
    }
}
