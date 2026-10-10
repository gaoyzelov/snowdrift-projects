package com.snowdrift.framework.cache.caffeine;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.caffeine.service.SnowdriftCaffeineCacheServiceImpl;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowdriftCaffeineCacheServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Caffeine 本地缓存实现单元测试（真实 Caffeine，无外部依赖）
 * @since 1.0.0
 */
class SnowdriftCaffeineCacheServiceImplTest {

    public static class User {
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

    private static SnowdriftCacheProperties properties() {
        SnowdriftCacheProperties properties = new SnowdriftCacheProperties();
        properties.setKeyPrefix("");
        properties.setKeyTtl(Duration.ofHours(1));
        return properties;
    }

    private static SnowdriftCaffeineCacheServiceImpl service() {
        return new SnowdriftCaffeineCacheServiceImpl(properties(), new JacksonCacheSerializer());
    }

    private static SnowdriftCaffeineCacheServiceImpl service(SnowdriftCacheProperties properties) {
        return new SnowdriftCaffeineCacheServiceImpl(properties, new JacksonCacheSerializer());
    }

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private static void awaitExpiry(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Nested
    @DisplayName("读写与过期")
    class ReadWrite {

        @Test
        @DisplayName("put/get 往返，未指定 TTL 时按全局 keyTtl 过期")
        void defaultTtlApplies() throws Exception {
            SnowdriftCacheProperties properties = properties();
            properties.setKeyTtl(Duration.ofMillis(150));
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            service.put("user:1", user(1L, "张三"));
            assertThat(service.get("user:1", User.class).getName()).isEqualTo("张三");

            awaitExpiry(400);
            assertThat(service.get("user:1", User.class)).isNull();
        }

        @Test
        @DisplayName("带 TTL 写入覆盖全局默认值：短 TTL 先过期，长 TTL 仍存在")
        void explicitTtlWins() throws Exception {
            SnowdriftCaffeineCacheServiceImpl service = service();

            service.put("short", "v", Duration.ofMillis(100));
            service.put("long", "v", Duration.ofHours(1));

            awaitExpiry(300);
            assertThat(service.get("short", String.class)).isNull();
            assertThat(service.get("long", String.class)).isEqualTo("v");
        }

        @Test
        @DisplayName("key 前缀生效")
        void keyPrefix() {
            SnowdriftCacheProperties properties = properties();
            properties.setKeyPrefix("app");
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            service.put("user:1", "v");

            assertThat(service.exists("user:1")).isTrue();
            assertThat(service.getExpire("user:1")).isNotEqualTo(-2L);
        }

        @Test
        @DisplayName("读取类型与写入类型不匹配时抛 BizException（Jackson 严格）")
        void wrongTypeReadFails() {
            SnowdriftCaffeineCacheServiceImpl service = service();
            service.put("user:1", user(1L, "张三"));

            // 写入的是 POJO（JSON 对象），按 String 读取时 Jackson 无法映射，包装为 BizException
            assertThatThrownBy(() -> service.get("user:1", String.class))
                    .isInstanceOf(BizException.class)
                    .hasMessage("缓存反序列化失败");
        }
    }

    @Nested
    @DisplayName("putIfAbsent / 删除 / 存在性")
    class Operations {

        @Test
        @DisplayName("putIfAbsent：首次成功，重复返回 false 且不覆盖旧值")
        void putIfAbsent() {
            SnowdriftCaffeineCacheServiceImpl service = service();

            assertThat(service.putIfAbsent("k", "first", Duration.ofMinutes(1))).isTrue();
            assertThat(service.putIfAbsent("k", "second", Duration.ofMinutes(1))).isFalse();
            assertThat(service.get("k", String.class)).isEqualTo("first");
        }

        @Test
        @DisplayName("putIfAbsent 不带 TTL 时按全局 keyTtl 过期")
        void putIfAbsentDefaultTtl() throws Exception {
            SnowdriftCacheProperties properties = properties();
            properties.setKeyTtl(Duration.ofMillis(150));
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            assertThat(service.putIfAbsent("k", "v")).isTrue();
            assertThat(service.exists("k")).isTrue();

            awaitExpiry(400);
            assertThat(service.exists("k")).isFalse();
        }

        @Test
        @DisplayName("delete / batchDelete 返回真实删除结果")
        void deletes() {
            SnowdriftCaffeineCacheServiceImpl service = service();
            service.put("a", "1");
            service.put("b", "2");

            assertThat(service.delete("a")).isTrue();
            assertThat(service.delete("a")).isFalse();
            assertThat(service.batchDelete(List.of("b", "absent"))).isEqualTo(1L);
        }

        @Test
        @DisplayName("exists 与 clear 语义")
        void exists() {
            SnowdriftCaffeineCacheServiceImpl service = service();

            assertThat(service.exists("k")).isFalse();
            service.put("k", "v");
            assertThat(service.exists("k")).isTrue();
        }
    }

    @Nested
    @DisplayName("per-key TTL：expire / getExpire")
    class Ttl{

        @Test
        @DisplayName("expire 存在的键返回 true 并生效；不存在的键返回 false")
        void expire() throws Exception {
            SnowdriftCaffeineCacheServiceImpl service = service();
            service.put("k", "v", Duration.ofHours(1));

            assertThat(service.expire("k", Duration.ofMillis(100))).isTrue();
            assertThat(service.expire("absent", Duration.ofMinutes(1))).isFalse();

            awaitExpiry(300);
            assertThat(service.exists("k")).isFalse();
        }

        @Test
        @DisplayName("全局 key-ttl 非正（永不过期）时 getExpire 返回 -1，而不是 4.6e9（回归：接口契约）")
        void getExpireForNeverExpiringEntry() {
            SnowdriftCacheProperties properties = properties();
            properties.setKeyTtl(Duration.ZERO);
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            service.put("k", "v");

            assertThat(service.getExpire("k")).isEqualTo(-1L);
            assertThat(service.exists("k")).isTrue();
        }

        @Test
        @DisplayName("超长 TTL（纳秒溢出，如 1000 年）按永不过期处理，而不是让写入抛 ArithmeticException")
        void oversizedGlobalTtlDoesNotOverflow() {
            SnowdriftCacheProperties properties = properties();
            // Duration.toNanos() 上限约 292 年，这里显式越过该上限
            properties.setKeyTtl(Duration.ofDays(365_000));
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            service.put("k", "v");

            assertThat(service.get("k", String.class)).isEqualTo("v");
            assertThat(service.getExpire("k")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("getExpire 返回剩余秒数；键不存在返回 -2")
        void getExpire() {
            SnowdriftCaffeineCacheServiceImpl service = service();
            service.put("k", "v", Duration.ofMinutes(5));

            assertThat(service.getExpire("k")).isBetween(1L, 300L);
            assertThat(service.getExpire("absent")).isEqualTo(-2L);
        }
    }

    @Nested
    @DisplayName("容量与不支持的操作")
    class Limits {

        @Test
        @DisplayName("超过 maxSize 时按 Caffeine 策略淘汰")
        void evictsBeyondMaxSize() {
            SnowdriftCacheProperties properties = properties();
            properties.setMaxSize(1);
            SnowdriftCaffeineCacheServiceImpl service = service(properties);

            service.put("k1", "v1", Duration.ofHours(1));
            service.put("k2", "v2", Duration.ofHours(1));

            assertThat(service.get("k2", String.class)).isEqualTo("v2");
            assertThat(service.exists("k1")).isFalse();
        }

        @Test
        @DisplayName("Hash 操作与 increment 不支持，抛 UnsupportedOperationException（README 已声明）")
        void unsupportedOperations() {
            SnowdriftCaffeineCacheServiceImpl service = service();
            service.put("k", "v");

            assertThatThrownBy(() -> service.hget("k", "f", String.class))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("hget");
            assertThatThrownBy(() -> service.hput("k", "f", "v"))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("hput");
            assertThatThrownBy(() -> service.hdelete("k", "f"))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("hdelete");
            assertThatThrownBy(() -> service.increment("k", Duration.ofMinutes(1)))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("increment");
        }
    }
}
