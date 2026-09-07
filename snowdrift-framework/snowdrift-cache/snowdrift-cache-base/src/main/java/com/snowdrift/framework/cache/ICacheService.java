package com.snowdrift.framework.cache;

import java.time.Duration;
import java.util.Collection;

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
     * 获取 Hash 中的单个字段
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段名
     * @param type    返回值类型
     * @param <T>     泛型
     * @return 缓存值，不存在返回 null
     */
    <T> T hget(String key, String hashKey, Class<T> type);

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
     * 设置 Hash 中的单个字段
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段名
     * @param value   缓存值
     */
    void hput(String key, String hashKey, Object value);


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
     * 删除 Hash 中的单个字段
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段名
     */
    void hdelete(String key, String hashKey);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 实际删除的数量
     */
    long batchDelete(Collection<String> keys);

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
     * @return 剩余秒数，-1 表示永不过期
     */
    long getExpire(String key);


    /**
     * 原子自增并刷新过期时间（Lua INCR+EXPIRE 单次往返原子执行）
     * <p>适用于限流计数、失败次数统计等场景；每次调用都会刷新 TTL，形成滚动窗口</p>
     *
     * @param key 缓存键
     * @param ttl 过期时间（每次自增后刷新），null 表示使用全局默认 TTL
     * @return 自增后的值，key 不存在时从 0 开始计为 1
     */
    long increment(String key, Duration ttl);
}
