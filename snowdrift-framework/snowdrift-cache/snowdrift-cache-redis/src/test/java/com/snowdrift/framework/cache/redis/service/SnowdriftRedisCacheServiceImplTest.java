package com.snowdrift.framework.cache.redis.service;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.FastJson2CacheSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SnowdriftRedisCacheServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Redis 缓存实现单元测试（mock RedisTemplate，验证命令映射与参数换算）
 * @since 1.0.0
 */
class SnowdriftRedisCacheServiceImplTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);

    private SnowdriftRedisCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        SnowdriftCacheProperties properties = new SnowdriftCacheProperties();
        properties.setKeyPrefix("");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        service = new SnowdriftRedisCacheServiceImpl(properties, new FastJson2CacheSerializer(), redisTemplate);
    }

    @Test
    @DisplayName("get：读取字符串并反序列化为目标类型")
    void get() {
        when(valueOperations.get("user:1")).thenReturn("{\"id\":1,\"name\":\"张三\"}");

        Sample back = service.get("user:1", Sample.class);

        assertThat(back.getId()).isEqualTo(1L);
        assertThat(back.getName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("put：不带 TTL 时执行裸 SET（Redis 语义为永不过期）")
    void putWithoutTtl() {
        service.put("k", "v");

        verify(valueOperations).set("k", "\"v\"");
    }

    @Test
    @DisplayName("put：带 TTL 时把默认 TTL 一并传给 Redis")
    void putWithTtl() {
        service.put("k", "v", Duration.ofMinutes(5));
        verify(valueOperations).set("k", "\"v\"", Duration.ofMinutes(5));

        service.put("k2", "v", null);
        verify(valueOperations).set("k2", "\"v\"", Duration.ofHours(1));
    }

    @Test
    @DisplayName("putIfAbsent：直接使用 Redis SETNX 结果")
    void putIfAbsent() {
        when(valueOperations.setIfAbsent("k", "\"v\"")).thenReturn(true);
        assertThat(service.putIfAbsent("k", "v")).isTrue();

        when(valueOperations.setIfAbsent("k", "\"v\"", Duration.ofMinutes(1))).thenReturn(false);
        assertThat(service.putIfAbsent("k", "v", Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("delete / batchDelete / exists 透传 Redis 命令结果")
    void deletesAndExists() {
        when(redisTemplate.delete("k")).thenReturn(true);
        assertThat(service.delete("k")).isTrue();

        when(redisTemplate.delete(List.of("a", "b"))).thenReturn(2L);
        assertThat(service.batchDelete(List.of("a", "b"))).isEqualTo(2L);

        when(redisTemplate.hasKey("k")).thenReturn(true);
        assertThat(service.exists("k")).isTrue();
    }

    @Test
    @DisplayName("expire / getExpire：单位与哨兵值透传（-1 永不过期 / -2 不存在）")
    void expireAndGetExpire() {
        when(redisTemplate.expire("k", Duration.ofMinutes(1))).thenReturn(Boolean.TRUE);
        assertThat(service.expire("k", Duration.ofMinutes(1))).isTrue();

        when(redisTemplate.getExpire("k", TimeUnit.SECONDS)).thenReturn(59L);
        assertThat(service.getExpire("k")).isEqualTo(59L);

        when(redisTemplate.getExpire("absent", TimeUnit.SECONDS)).thenReturn(-2L);
        assertThat(service.getExpire("absent")).isEqualTo(-2L);
    }

    @Test
    @DisplayName("hash 操作映射到 Redis Hash 命令")
    void hashOperations() {
        service.hput("h", "f", "v");
        verify(hashOperations).put("h", "f", "\"v\"");

        when(hashOperations.get("h", "f")).thenReturn("\"v\"");
        assertThat(service.hget("h", "f", String.class)).isEqualTo("v");

        when(hashOperations.delete("h", "f")).thenReturn(1L);
        service.hdelete("h", "f");
    }

    @Test
    @DisplayName("increment：Lua 一次性 INCR+EXPIRE，TTL 按毫秒向上取整为秒（至少 1 秒）")
    void incrementRoundsSecondsUp() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        assertThat(service.increment("counter", Duration.ofMillis(1500))).isEqualTo(1L);
        assertThat(capturedSeconds()).isEqualTo("2");

        service.increment("counter", Duration.ofMillis(100));
        assertThat(capturedSeconds()).isEqualTo("1");

        service.increment("counter", Duration.ofSeconds(60));
        assertThat(capturedSeconds()).isEqualTo("60");

    }

    @Test
    @DisplayName("increment：非正 TTL 在服务层即被拒绝（不再夹到 1 秒或抛后端原始异常）")
    void incrementRejectsNonPositiveTtl() {
        assertThatThrownBy(() -> service.increment("counter", Duration.ZERO))
                .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
        assertThatThrownBy(() -> service.increment("counter", Duration.ofSeconds(-5)))
                .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
    }

    private String capturedSeconds() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate, org.mockito.Mockito.atLeastOnce())
                .execute(any(RedisScript.class), anyList(), captor.capture());
        return String.valueOf(captor.getValue());
    }

    @Test
    @DisplayName("非正 TTL 的 increment 仍走统一校验（null 直接拒绝）")
    void incrementRejectsNullTtl() {
        assertThatThrownBy(() -> service.increment("k", null))
                .isInstanceOf(BizException.class).hasMessage("缓存TTL不能为空");
    }

    @Test
    @DisplayName("key 前缀拼接到所有命令的 key 上")
    void keyPrefix() {
        SnowdriftCacheProperties properties = new SnowdriftCacheProperties();
        properties.setKeyPrefix("app");
        SnowdriftRedisCacheServiceImpl prefixed =
                new SnowdriftRedisCacheServiceImpl(properties, new FastJson2CacheSerializer(), redisTemplate);

        prefixed.put("user:1", "v");

        verify(valueOperations).set(eq("app:user:1"), eq("\"v\""));
    }

    @Test
    @DisplayName("读取不存在的 key 返回 null")
    void missingKey() {
        when(valueOperations.get("absent")).thenReturn(null);

        assertThat(service.get("absent", Sample.class)).isNull();
    }

    public static class Sample {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
