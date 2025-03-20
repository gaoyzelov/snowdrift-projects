package com.snowdrift.cache.redis.service;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IRedisCacheService
 *
 * @author gaoye
 * @date 2025/03/20 10:18:02
 * @description Redis缓存服务
 * @since 1.0.0
 */
public interface IRedisCacheService {

    RedisTemplate<String, Object> getRedisTemplate();

    void set(String key, Object value);

    boolean setNx(String key, Object value);

    boolean setBit(String key, long index);

    boolean getBit(String key, long index);

    long bitCount(String key);

    Object get(String key);

    <T> T get(String key, Class<T> clazz) throws ClassCastException;

    long getExpire(String key);

    void setExpire(String key, Object value, long expire);

    boolean setNxExpire(String key, Object value, Long timeout);

    boolean hasKey(String key);

    boolean delete(String key);

    long delete(Collection<String> keys);

    long getIncr(String key);

    long getIncr(String key, long delta);

    long getDecr(String key);

    long getDecr(String key, long delta);

    void hSet(String key, String hashKey, Object object);

    boolean hSetExpire(String key, String hashKey, Object object, Integer expire);

    void hSetMap(String key, Map<String, Object> map);

    boolean hSetIfAbsent(String key, String hashKey, Object object);

    Object hGet(String key, String hashKey);

    <T> T hGet(String key, String hashKey, Class<T> clazz) throws ClassCastException;

    Map<Object, Object> hGetMap(String key);

    boolean hDeleteKey(String key);

    long hDeleteKey(String key, String hashKey);

    boolean hHasKey(String key);

    boolean hHasKey(String key, String hashKey);

    long setList(String key, List<Object> values);

    boolean setListExpire(String key, List<Object> values, long timeout);

    List<Object> getList(String key);

    List<Object> getList(String key, long start, long end);

    long getListSize(String key);

    long delListValue(String key, Object value);

    boolean zSet(String key, Object value, double score);

    Long zRank(String key, Object value);

    Long zRevRank(String key, Object value);

    Set<Object> zRange(String key, long start, long end);

    Set<Object> zRevRange(String key, long start, long end);

    <T> T eval(String script, Class<T> clazz, List<String> keys, Object... args);
}