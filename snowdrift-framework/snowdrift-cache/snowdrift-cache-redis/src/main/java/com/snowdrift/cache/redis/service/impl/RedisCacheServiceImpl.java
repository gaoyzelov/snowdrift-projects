package com.snowdrift.cache.redis.service.impl;

import com.snowdrift.cache.redis.service.IRedisCacheService;
import lombok.NonNull;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisCacheServiceImpl
 *
 * @author gaoye
 * @date 2025/03/20 10:18:49
 * @description Redis 缓存服务实现类
 * @since 1.0.0
 */
public class RedisCacheServiceImpl implements IRedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheServiceImpl(@NonNull RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 缓存键值
     *
     * @param key   键
     * @param value 值
     */
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存数据：key不存在才会设置
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return ture-缓存成功 false-缓存失败
     */
    @Override
    public boolean setNx(String key, Object value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(result);
    }

    /**
     * bitmap设置值
     *
     * @param key   键
     * @param index 下标
     */
    @Override
    public boolean setBit(String key, long index) {
        Boolean result = redisTemplate.opsForValue().setBit(key, index, true);
        return Boolean.TRUE.equals(result);
    }


    /**
     * bitmap设置值
     *
     * @param key   键
     * @param index 下标
     */
    @Override
    public boolean getBit(String key, long index) {
        Boolean result = redisTemplate.opsForValue().getBit(key, index);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 统计数量
     *
     * @param key 键
     * @return 数量
     */
    @Override
    public long bitCount(String key) {
        Long count = redisTemplate.execute((RedisCallback<Long>) conn -> conn.bitCount(key.getBytes()));
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取值
     *
     * @param key   键
     * @param clazz 值类型
     * @param <T>   值类型
     * @return 值
     */
    @Override
    public <T> T get(String key, Class<T> clazz) throws ClassCastException {
        Object o = redisTemplate.opsForValue().get(key);
        return Objects.isNull(o) ? null : clazz.cast(o);
    }

    /**
     * 获取键过期时间
     *
     * @param key 键值
     * @return 时间戳
     */
    @Override
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return Objects.isNull(expire) ? 0 : expire;
    }


    /**
     * 缓存键值，并设置过期时间
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时间，单位：秒
     */
    @Override
    public void setExpire(String key, Object value, long expire) {
        redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
    }

    /**
     * 缓存数据并设置过期时间：key不存在才会设置
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @return true-缓存成功 false-缓存失败
     */
    @Override
    public boolean setNxExpire(String key, Object value, Long timeout) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return true 存在，false 不存在
     */
    @Override
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    @Override
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 批量删除键
     *
     * @param keys 键集合
     * @return 删除数量
     */
    @Override
    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 获取键自增长值
     *
     * @param key 键
     * @return 自增后的值
     */
    @Override
    public long getIncr(String key) {
        Long increment = redisTemplate.opsForValue().increment(key);
        return Objects.isNull(increment) ? 0 : increment;
    }

    /**
     * 获取键自增长值
     *
     * @param key   键
     * @param delta 自增值
     * @return 自增后的值
     */
    @Override
    public long getIncr(String key, long delta) {
        Long increment = redisTemplate.opsForValue().increment(key, delta);
        return Objects.isNull(increment) ? 0 : increment;
    }

    /**
     * 获取键自减值
     *
     * @param key 键
     * @return 自减后的值
     */
    @Override
    public long getDecr(String key) {
        Long decrement = redisTemplate.opsForValue().decrement(key);
        return Objects.isNull(decrement) ? 0 : decrement;
    }

    /**
     * 获取键自减值
     *
     * @param key   键
     * @param delta 自减值
     * @return 自减后的值
     */
    @Override
    public long getDecr(String key, long delta) {
        Long decrement = redisTemplate.opsForValue().decrement(key, delta);
        return Objects.isNull(decrement) ? 0 : decrement;
    }

    /**
     * 设置对象
     *
     * @param key     key
     * @param hashKey hashKey
     * @param object  对象
     */
    @Override
    public void hSet(String key, String hashKey, Object object) {
        redisTemplate.opsForHash().put(key, hashKey, object);
    }

    /**
     * 设置对象
     *
     * @param key     key
     * @param hashKey hashKey
     * @param object  对象
     * @param expire  过期时间，单位秒
     */
    @Override
    public boolean hSetExpire(String key, String hashKey, Object object, Integer expire) {
        redisTemplate.opsForHash().put(key, hashKey, object);
        Boolean result = redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置HashMap
     *
     * @param key key
     * @param map map值
     */
    @Override
    public void hSetMap(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * key不存在时设置值
     *
     * @param key     键
     * @param hashKey hash键
     * @param object  存储对象
     */
    @Override
    public boolean hSetIfAbsent(String key, String hashKey, Object object) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, object);
    }

    /**
     * 获取Hash值
     *
     * @param key     键
     * @param hashKey hash键
     * @return 返回值
     */
    @Override
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取Hash值
     *
     * @param key     键
     * @param hashKey hash键
     * @return 返回值
     */
    @Override
    public <T> T hGet(String key, String hashKey, Class<T> clazz) throws ClassCastException {
        Object o = redisTemplate.opsForHash().get(key, hashKey);
        return Objects.isNull(o) ? null : clazz.cast(o);
    }

    /**
     * 获取key的所有值
     *
     * @param key 键
     * @return 返回值
     */
    @Override
    public Map<Object, Object> hGetMap(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除key的所有值
     *
     * @param key 键
     */
    @Override
    public boolean hDeleteKey(String key) {
        Boolean result = redisTemplate.opsForHash().getOperations().delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 删除hash
     *
     * @param key     键
     * @param hashKey hash键
     */
    @Override
    public long hDeleteKey(String key, String hashKey) {
        Long count = redisTemplate.opsForHash().delete(key, hashKey);
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 判断key下是否有值
     *
     * @param key 键
     */
    @Override
    public boolean hHasKey(String key) {
        Boolean result = redisTemplate.opsForHash().getOperations().hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 判断key和hasKey下是否有值
     *
     * @param key     键
     * @param hashKey hash键
     */
    @Override
    public boolean hHasKey(String key, String hashKey) {
        Boolean result = redisTemplate.opsForHash().hasKey(key, hashKey);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 缓存List数据
     *
     * @param key    键
     * @param values List数据
     */
    @Override
    public long setList(String key, List<Object> values) {
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 缓存List数据并设置过期时间
     *
     * @param key     键
     * @param values  List数据
     * @param timeout 过期时长
     */
    @Override
    public boolean setListExpire(String key, List<Object> values, long timeout) {
        redisTemplate.opsForList().rightPushAll(key, values);
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取缓存的List数据
     *
     * @param key 键值
     * @return List数据
     */
    @Override
    public List<Object> getList(String key) {
        return getList(key, 0, -1);
    }

    /**
     * 获取缓存的List数据
     *
     * @param key   键值
     * @param start 开始位置
     * @param end   结束位置 -1表示全部
     */
    @Override
    public List<Object> getList(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取缓存的list长度
     *
     * @param key 键值
     * @return 长度
     */
    @Override
    public long getListSize(String key) {
        Long count = redisTemplate.opsForList().size(key);
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 移除缓存的列表中的值
     *
     * @param key   key 缓存列表的key
     * @param value 待移除的值
     */
    @Override
    public long delListValue(String key, Object value) {
        Long count = redisTemplate.opsForList().remove(key, 0, value);
        return Objects.isNull(count) ? 0 : count;
    }

    /**
     * 添加有序集合
     *
     * @param key   key
     * @param value 值
     * @param score 分数
     */
    @Override
    public boolean zSet(String key, Object value, double score) {
        Boolean result = redisTemplate.opsForZSet().add(key, value, score);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 查询有序集合的排名-升序
     *
     * @param key   key
     * @param value 值
     */
    @Override
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * 查询有序集合的排名-降序
     *
     * @param key   key
     * @param value 值
     */
    @Override
    public Long zRevRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 查询有序集合的元素-升序
     *
     * @param key   key
     * @param start 开始位置
     * @param end   结束位置
     */
    @Override
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 查询有序集合的元素-降序
     *
     * @param key   key
     * @param start 开始位置
     * @param end   结束位置
     */
    @Override
    public Set<Object> zRevRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 执行lua脚本
     *
     * @param script lua脚本
     * @param clazz  返回类型
     * @param keys   key集合
     * @param args   参数
     * @return 返回值
     */
    @Override
    public <T> T eval(String script, Class<T> clazz, List<String> keys, Object... args) {
        return redisTemplate.execute(RedisScript.of(script, clazz), keys, args);
    }
}