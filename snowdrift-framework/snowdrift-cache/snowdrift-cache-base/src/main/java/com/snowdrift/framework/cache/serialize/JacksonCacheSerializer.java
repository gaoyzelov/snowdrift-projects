package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Type;

/**
 * 基于 Jackson 的缓存序列化器，项目统一的缓存序列化实现。
 * <p>
 * 用于 {@code ICacheService}：读取时由调用方显式传入目标类型（{@code Class<T>} 或 {@code Type}），
 * 因此本序列化器<b>不写入类型元数据</b>，落盘无冗余。Spring Cache（{@code @Cacheable}）的值序列化
 * 扩展点是 {@code RedisSerializer<Object>}，拿不到方法返回类型，必须把类型随 JSON 落盘，请改用
 * {@link #cacheValueMapper()} 构建的 mapper。
 * </p>
 * <p>行为约定：</p>
 * <ul>
 *   <li>字段口径：只按字段读写（关闭 getter/计算属性探测），显式 {@code @JsonProperty} 仍生效、{@code @JsonIgnore} 仍排除</li>
 *   <li>键序：属性名与 Map 键均按字母序输出，保证 {@code HashMap} 等无序容器产出确定性 JSON</li>
 *   <li>日期时间：注册 {@link JavaTimeModule} 并关闭时间戳输出，java.time 类型统一按 ISO-8601 字符串落盘
 *       （如 {@code LocalDateTime} → {@code 2026-10-09T12:00:00}）</li>
 *   <li>空值：null 字段与 Map 中的 null 值都不写出（{@code NON_NULL}）</li>
 *   <li>宽容读取：忽略未知字段（{@code FAIL_ON_UNKNOWN_PROPERTIES=false}），兼容结构演进后的旧数据</li>
 * </ul>
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

    @Override
    protected <T> T doDeserialize(String json, Type type) throws Exception {
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type));
    }

    /**
     * 构建 {@code ICacheService} 使用的 mapper：不写类型元数据，读取类型由调用方提供
     */
    public static ObjectMapper defaultMapper() {
        return baseBuilder().build();
    }

    /**
     * 两个 mapper 共用的基础配置：{@code ICacheService} 路径直接 {@link #defaultMapper()} 构建，
     * Spring Cache 路径在此基础上追加类型信息。统一走 {@code JsonMapper.builder()} 链式配置，
     * 避免先 build 再突变已构建的实例
     */
    private static JsonMapper.Builder baseBuilder() {
        return JsonMapper.builder()
                // 反序列化遇到实体不存在字段不抛异常
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 关闭日期输出时间戳，输出 ISO 字符串
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 空值不序列化（值与内容均为 NON_NULL）
                .defaultPropertyInclusion(JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                // 字段口径：关闭 getter/计算属性探测、只按字段读写（显式 @JsonProperty 仍生效、@JsonIgnore 仍排除）
                .visibility(PropertyAccessor.ALL, Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, Visibility.ANY)
                // 属性名与 Map 键按字母序输出，保证无序容器产出确定性 JSON
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                // 注册 java.time 支持（java.time 类型按 ISO-8601 字符串落盘）
                .addModule(new JavaTimeModule());
    }

    /**
     * 构建供 Spring Cache（{@code @Cacheable}）使用的 mapper
     *
     * @return 已启用类型信息的 ObjectMapper
     */
    public static ObjectMapper cacheValueMapper() {
        @SuppressWarnings("deprecation")
        ObjectMapper mapper = baseBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY)
                .build();
        return mapper;
    }
}
