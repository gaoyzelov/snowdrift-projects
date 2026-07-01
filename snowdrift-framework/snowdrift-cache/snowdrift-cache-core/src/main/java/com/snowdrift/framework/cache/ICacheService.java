package com.snowdrift.framework.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

/**
 * 统一缓存操作接口
 * <p>
 * 屏蔽底层缓存实现差异（Caffeine / Redis / Redisson），
 * 提供统一的 key-value 缓存存取能力。
 * 配合 Spring Cache 注解（@Cacheable 等）使用，覆盖编程式缓存场景。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
public interface ICacheService {

    /**
     * 获取缓存
     *
     * @param key  缓存键
     * @param type 返回值类型
     * @param <T>  泛型
     * @return 缓存值，不存在返回 null
     */
    <T> T get(String key, Class<T> type);

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);


    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间，null 表示使用全局默认 TTL
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 仅当 key 不存在时设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return true=设置成功（key 之前不存在），false=key 已存在
     */
    boolean putIfAbsent(String key, Object value);

    /**
     * 仅当 key 不存在时设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间，null 表示使用全局默认 TTL
     * @return true=设置成功（key 之前不存在），false=key 已存在
     */
    boolean putIfAbsent(String key, Object value, Duration ttl);

    /**
     * 删除单个缓存
     *
     * @param key 缓存键
     * @return true=删除成功
     */
    boolean delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 实际删除的数量
     */
    long delete(Collection<String> keys);

    /**
     * 判断 key 是否存在
     *
     * @param key 缓存键
     * @return true=存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     *
     * @param key 缓存键
     * @param ttl 过期时间
     * @return true=设置成功
     */
    boolean expire(String key, Duration ttl);

    /**
     * 获取剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余秒数，-1 表示永不过期，-2 表示 key 不存在
     */
    long getExpire(String key);

    /**
     * 模糊匹配 key
     *
     * @param pattern 匹配模式（如 "user:*"）
     * @return 匹配的 key 集合
     */
    Set<String> keys(String pattern);
}
