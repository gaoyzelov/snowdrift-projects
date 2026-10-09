package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/**
 * 基于 Jackson 的缓存序列化器（用于 {@code ICacheService}，读取时由调用方显式传入目标类型）
 * <p>
 * 本序列化器<b>不写入类型元数据</b>：{@code ICacheService} 的读取方法均要求显式传入目标类型
 * （{@code Class<T>} 或 {@code Type}），类型信息由调用方提供，落盘时无需冗余。
 * </p>
 * <p>
 * Spring Cache（{@code @Cacheable}）的值序列化扩展点是 {@code RedisSerializer<Object>}，拿不到方法返回类型，
 * 必须把类型随 JSON 落盘，请改用 {@link #cacheValueMapper()} 构建的 mapper。
 * </p>
 * <p><b>与 {@link FastJson2CacheSerializer} 的等价约定（两个实现可互换，同一对象产出同一 JSON）：</b>
 * 字段口径（只按字段读写）、键序（字母序）、日期时间格式（{@code LocalDateTime} 与
 * {@code java.util.Date} 系输出 {@value #DATE_TIME_PATTERN}，其余保持 ISO 风格）三项均已对齐；
 * 且 {@code LocalDateTime}/{@code Date} 读取时同时接受空格与 ISO 两种写法，故两侧数据可互相读取。
 * </p>
 * <ul>
 *   <li>忽略未知字段（{@code FAIL_ON_UNKNOWN_PROPERTIES=false}），兼容结构演进</li>
 *   <li>日期不写时间戳（关闭 {@code WRITE_DATES_AS_TIMESTAMPS}）：{@code LocalDateTime} 与 {@code java.util.Date} 系
 *       按 {@value #DATE_TIME_PATTERN} 输出，其余 java.time 类型保持各自的 ISO 风格</li>
 *   <li>空值不序列化（{@code NON_NULL}）</li>
 * </ul>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
public class JacksonCacheSerializer extends AbstractCacheSerializer {

    /**
     * 与 fastjson2 对齐的日期时间格式（非 ISO，空格分隔）
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.ROOT);

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

    public static ObjectMapper defaultMapper() {
        ObjectMapper om = new ObjectMapper();
        // 反序列化遇到实体不存在字段不抛异常
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 关闭日期输出时间戳，输出ISO字符串
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 空值不序列化
        om.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        // 字段口径：关闭 getter/计算属性探测、只按字段读写，与 FastJson2CacheSerializer 的 FieldBased 对齐，
        // 保证两个序列化器对同一对象的 JSON 字段集一致（显式 @JsonProperty 仍生效、@JsonIgnore 仍排除）
        om.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        om.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        // 键序对齐 fastjson2 的默认输出（字母序），使两者产物可逐字比对
        om.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        om.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        // 注册JavaTimeModule，支持LocalDateTime/LocalDate等java.time类
        // 注意顺序：本模块必须排在下面的日期定制模块之前——同类型序列化器后注册者覆盖先注册者
        om.registerModule(new JavaTimeModule());
        // 定制与 fastjson2 输出不一致的两个类型（LocalDateTime / java.util.Date 系）
        om.registerModule(dateTimeModule());
        return om;
    }

    /**
     * 日期时间模块：抹平与 fastjson2 的输出差异
     * <p>{@code LocalDate}/{@code LocalTime}/{@code OffsetDateTime}/{@code Instant} 两库默认输出一致，无需定制；
     * 只有 {@code LocalDateTime} 与 {@code java.util.Date} 系（含 {@code sql.Timestamp}）不同：
     * fastjson2 输出 {@code yyyy-MM-dd HH:mm:ss}（JVM 默认时区），而 Jackson 输出 ISO。</p>
     */
    private static SimpleModule dateTimeModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new TolerantLocalDateTimeDeserializer());
        module.addSerializer(Date.class, new BeanStyleDateSerializer());
        module.addDeserializer(Date.class, new TolerantDateDeserializer());
        return module;
    }

    /**
     * {@code java.util.Date} 序列化：按 JVM 默认时区输出 {@code yyyy-MM-dd HH:mm:ss}（与 fastjson2 一致）
     */
    private static final class BeanStyleDateSerializer extends JsonSerializer<Date> {
        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(DATE_TIME_FORMATTER.format(value.toInstant().atZone(ZoneId.systemDefault())));
        }
    }

    /**
     * {@code LocalDateTime} 反序列化：同时接受空格（本框架标准）与 ISO {@code 'T'} 两种写法
     */
    private static final class TolerantLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            try {
                return text.contains("T") ? LocalDateTime.parse(text) : LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IOException("无法解析时间: " + text, e);
            }
        }
    }

    /**
     * {@code java.util.Date} 反序列化：同时接受空格（本标准）与 ISO 偏移/瞬时刻两种写法
     */
    private static final class TolerantDateDeserializer extends JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            try {
                if (text.contains("T")) {
                    return Date.from(text.endsWith("Z")
                            ? Instant.parse(text)
                            : OffsetDateTime.parse(text).toInstant());
                }
                return Date.from(LocalDateTime.parse(text, DATE_TIME_FORMATTER)
                        .atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new IOException("无法解析日期: " + text, e);
            }
        }
    }

    /**
     * 构建供 Spring Cache（{@code @Cacheable}）使用的 mapper：在 {@link #defaultMapper()} 基础上启用多态类型信息
     * <p>
     * 启用原因：Spring Cache 的值序列化扩展点是 {@code RedisSerializer<Object>}，拿不到方法声明的返回类型，
     * 类型信息只能随 JSON 落盘（{@code @class}）；否则反序列化会退化为 {@code LinkedHashMap}，
     * 并在调用方按返回类型强转时抛 {@code ClassCastException}。
     * </p>
     * <p><b>为何用 {@code EVERYTHING} 而不是 Spring 默认的 {@code NON_FINAL}：</b>Jackson 序列化根对象时
     * 依据的是<b>运行时类型</b>，而 {@code NON_FINAL} 不为 final 类型写入类型信息。因此
     * {@code List.of(..)}、{@code Map.of(..)}（运行时类是 final 的 JDK 不可变容器）以及 final 的返回类型
     * （如框架自身的 {@code Result<T>}）读取时会因缺少类型标记而抛异常。{@code EVERYTHING} 为全部非基本类型
     * 写入类型信息，上述写法均可正常往返（逐一实测过）。
     * </p>
     * <p>
     * 该常量被 Jackson 2.21 标记为 {@code @Deprecated}（未标 {@code forRemoval}，2.x 内不会被移除），
     * 原因是它对多数场景过于宽泛；此处属例外场景，且唯一相关的未废弃替代 {@code NON_FINAL_AND_ENUMS}
     * 同样漏掉 final 类型，故沿用。
     * </p>
     * <p><b>信任前提（务必确认）：</b>使用 accept-all 的多态类型校验器。反序列化会实例化 JSON 中
     * {@code @class} 指定的类，因此<b>缓存数据必须只能由本应用自身写入</b>；若缓存存在被外部篡改的可能，
     * 请勿使用本方法。
     * </p>
     *
     * @return 已启用类型信息的 ObjectMapper
     */
    public static ObjectMapper cacheValueMapper() {
        ObjectMapper om = defaultMapper();
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return om;
    }
}
