package com.snowdrift.framework.cache.serialize;

/**
 * 缓存序列化器接口
 * <p>
 * 将 Java 对象序列化为 JSON 字符串以及从 JSON 字符串反序列化。
 * 所有缓存后端（Caffeine / Redis / Redisson）共享同一套序列化机制，
 * 确保切换后端时数据格式一致。
 * </p>
 * <p>
 * 框架默认提供两种实现：
 * <ul>
 *   <li>{@link JacksonCacheSerializer} — 基于 Jackson，默认启用</li>
 *   <li>{@link FastJson2CacheSerializer} — 基于 Fastjson2</li>
 * </ul>
 * 通过 {@code snowdrift.cache.serializer=jackson|fastjson2} 切换。
 * 消费者也可自行实现此接口并注册为 Spring Bean 以完全自定义序列化行为。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
public interface CacheSerializer {

    /**
     * 序列化为 JSON 字符串
     *
     * @param value 待序列化的对象
     * @return JSON 字符串，value 为 null 时返回 null
     */
    String serialize(Object value);

    /**
     * 反序列化为目标类型
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  泛型
     * @return 反序列化后的对象，json 为 null 时返回 null
     */
    <T> T deserialize(String json, Class<T> type);
}
