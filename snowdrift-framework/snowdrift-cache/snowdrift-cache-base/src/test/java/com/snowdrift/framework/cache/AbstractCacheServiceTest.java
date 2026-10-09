package com.snowdrift.framework.cache;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.FastJson2CacheSerializer;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractCacheServiceTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 缓存服务抽象基类单元测试（参数校验、key 前缀、TTL 兜底、序列化委派）
 * @since 1.0.0
 */
class AbstractCacheServiceTest {

    /** 内存假实现：记录收到的真实 key 与 TTL，用于校验基类行为 */
    static class FakeCacheService extends AbstractCacheService {

        final Map<String, String> store = new LinkedHashMap<>();
        final List<String> receivedKeys = new ArrayList<>();
        Duration lastPutTtl;
        boolean usedTwoArgPut;
        boolean usedTwoArgPutIfAbsent;
        long batchDeleteResult = 0L;

        FakeCacheService(SnowdriftCacheProperties properties, ICacheSerializer serializer) {
            super(properties, serializer);
        }

        @Override
        protected String doGet(String key) {
            receivedKeys.add(key);
            return store.get(key);
        }

        @Override
        protected String doHget(String key, String hashKey) {
            receivedKeys.add(key);
            return store.get(key + "#" + hashKey);
        }

        @Override
        protected void doPut(String key, String value) {
            receivedKeys.add(key);
            usedTwoArgPut = true;
            store.put(key, value);
        }

        @Override
        protected void doPut(String key, String value, Duration ttl) {
            receivedKeys.add(key);
            lastPutTtl = ttl;
            store.put(key, value);
        }

        @Override
        protected void doHput(String key, String hashKey, String value) {
            receivedKeys.add(key);
            store.put(key + "#" + hashKey, value);
        }

        @Override
        protected boolean doPutIfAbsent(String key, String value) {
            receivedKeys.add(key);
            usedTwoArgPutIfAbsent = true;
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        protected boolean doPutIfAbsent(String key, String value, Duration ttl) {
            receivedKeys.add(key);
            lastPutTtl = ttl;
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        protected boolean doDelete(String key) {
            receivedKeys.add(key);
            return store.remove(key) != null;
        }

        @Override
        protected boolean doHdelete(String key, String hashKey) {
            receivedKeys.add(key);
            return store.remove(key + "#" + hashKey) != null;
        }

        @Override
        protected long doBatchDelete(List<String> keys) {
            receivedKeys.addAll(keys);
            long count = 0;
            for (String key : keys) {
                if (store.remove(key) != null) {
                    count++;
                }
            }
            batchDeleteResult = count;
            return count;
        }

        @Override
        protected boolean doExists(String key) {
            receivedKeys.add(key);
            return store.containsKey(key);
        }

        @Override
        protected boolean doExpire(String key, Duration ttl) {
            receivedKeys.add(key);
            return store.containsKey(key);
        }

        @Override
        protected long doGetExpire(String key) {
            receivedKeys.add(key);
            return store.containsKey(key) ? 60L : -2L;
        }

        @Override
        protected long doIncrement(String key, Duration ttl) {
            receivedKeys.add(key);
            return Long.parseLong(store.merge(key, "1", (a, b) -> String.valueOf(Long.parseLong(a) + 1)));
        }
    }

    /** 供序列化往返使用的样本对象 */
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

    private static Sample sample() {
        Sample sample = new Sample();
        sample.setId(1L);
        sample.setName("n");
        return sample;
    }

    private static SnowdriftCacheProperties properties(String keyPrefix) {
        SnowdriftCacheProperties properties = new SnowdriftCacheProperties();
        properties.setKeyPrefix(keyPrefix);
        return properties;
    }

    private FakeCacheService service(String keyPrefix) {
        return new FakeCacheService(properties(keyPrefix), new FastJson2CacheSerializer());
    }

    @Nested
    @DisplayName("key 前缀")
    class KeyPrefix {

        @Test
        @DisplayName("未配置前缀时使用裸 key")
        void withoutPrefix() {
            FakeCacheService service = service("");
            Sample sample = new Sample();
            sample.setId(1L);

            service.put("user:1", sample);
            service.get("user:1", Sample.class);

            assertThat(service.receivedKeys).containsExactly("user:1", "user:1");
        }

        @Test
        @DisplayName("配置前缀后统一拼成 prefix:key")
        void withPrefix() {
            FakeCacheService service = service("app");
            Sample sample = new Sample();
            sample.setId(1L);

            service.put("user:1", sample);
            service.delete("user:1");
            service.batchDelete(List.of("a", "b"));

            assertThat(service.receivedKeys).containsExactly("app:user:1", "app:user:1", "app:a", "app:b");
        }
    }

    @Nested
    @DisplayName("序列化委派")
    class Serialization {

        @Test
        @DisplayName("值经序列化器写入，读取时按传入类型还原")
        void roundTrip() {
            FakeCacheService service = service("");
            Sample sample = new Sample();
            sample.setId(9L);
            sample.setName("缓存");

            service.put("k", sample);

            assertThat(service.store.get("k")).isEqualTo("{\"id\":9,\"name\":\"缓存\"}");
            Sample back = service.get("k", Sample.class);
            assertThat(back.getId()).isEqualTo(9L);
            assertThat(back.getName()).isEqualTo("缓存");
        }

        @Test
        @DisplayName("泛型重载：key 前缀生效，集合元素还原为目标类型")
        void typeOverload() throws Exception {
            FakeCacheService service = service("app");

            service.put("list", java.util.List.of(sample()));

            java.lang.reflect.Type listType =
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Sample>>() {
                    }.getType();
            java.util.List<?> back = service.get("list", listType);

            assertThat(service.receivedKeys).contains("app:list");
            assertThat(back).hasSize(1);
            assertThat(back.get(0)).isInstanceOf(Sample.class);
            assertThat(((Sample) back.get(0)).getName()).isEqualTo("n");

            // 用 Class<T> 读同一个 key 只能拿到 Map 元素（这正是泛型重载要解决的问题）
            assertThat(service.get("list", java.util.List.class).get(0)).isNotInstanceOf(Sample.class);
        }

        @Test
        @DisplayName("泛型重载：目标类型为 null 时抛 BizException")
        void typeOverloadRejectsNullType() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.get("k", (java.lang.reflect.Type) null))
                    .isInstanceOf(BizException.class).hasMessage("目标类型不能为空");
        }

        @Test
        @DisplayName("key 不存在时直接返回 null，不做反序列化")
        void missingKeyReturnsNull() {
            FakeCacheService service = service("");

            assertThat(service.get("absent", Sample.class)).isNull();
            assertThat(service.hget("absent", "f", Sample.class)).isNull();
        }

        @Test
        @DisplayName("值为 null 一律拒绝（避免写入后读取语义不清）")
        void rejectsNullValue() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.put("k", null))
                    .isInstanceOf(BizException.class).hasMessage("缓存值不能为空");
            assertThatThrownBy(() -> service.put("k", null, Duration.ofMinutes(1)))
                    .isInstanceOf(BizException.class).hasMessage("缓存值不能为空");
            assertThatThrownBy(() -> service.putIfAbsent("k", null))
                    .isInstanceOf(BizException.class).hasMessage("缓存值不能为空");
            assertThatThrownBy(() -> service.hput("k", "f", null))
                    .isInstanceOf(BizException.class).hasMessage("缓存值不能为空");
        }
    }

    @Nested
    @DisplayName("TTL 兜底")
    class Ttl {

        @Test
        @DisplayName("显式 TTL 优先，未指定时回落全局默认值")
        void explicitTtlWins() {
            FakeCacheService service = service("");
            service.put("k", "v", Duration.ofSeconds(30));
            assertThat(service.lastPutTtl).isEqualTo(Duration.ofSeconds(30));

            service.put("k", "v", null);
            assertThat(service.lastPutTtl).isEqualTo(service.properties.getKeyTtl());
        }

        @Test
        @DisplayName("putIfAbsent：默认 TTL 非空时走带 TTL 的实现")
        void putIfAbsentUsesDefaultTtl() {
            FakeCacheService service = service("");
            service.putIfAbsent("k", "v", null);

            assertThat(service.usedTwoArgPutIfAbsent).isFalse();
            assertThat(service.lastPutTtl).isEqualTo(service.properties.getKeyTtl());
        }

        @Test
        @DisplayName("全局默认 TTL 为 null 时走不带 TTL 的实现（各后端自持默认值）")
        void fallsBackToNoTtlOverload() {
            SnowdriftCacheProperties props = properties("");
            props.setKeyTtl(null);
            FakeCacheService service = new FakeCacheService(props, new FastJson2CacheSerializer());

            service.putIfAbsent("k", "v", null);
            assertThat(service.usedTwoArgPutIfAbsent).isTrue();

            service.put("k2", "v", null);
            assertThat(service.lastPutTtl).isNull();
            assertThat(service.usedTwoArgPut).isFalse();
        }
    }

    @Nested
    @DisplayName("参数校验")
    class Validation {

        @Test
        @DisplayName("需要 key 的方法在 blank/null 时统一抛 BizException")
        void rejectsBlankKeys() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.get(" ", Sample.class))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.put(null, "v"))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.hput("k", " ", "v"))
                    .isInstanceOf(BizException.class).hasMessage("缓存哈希键不能为空");
            assertThatThrownBy(() -> service.exists(null))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.expire(null, Duration.ofMinutes(1)))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.getExpire(""))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.increment(null, Duration.ofMinutes(1)))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.batchDelete(List.of()))
                    .isInstanceOf(BizException.class).hasMessage("缓存键集合不能为空");
        }

        @Test
        @DisplayName("delete/batchDelete 的 key 同样做非空校验（此前缺失：null 会 NPE 或被拼成 prefix:null）")
        void rejectsBlankKeysForDelete() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.delete(null))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.delete(" "))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
            assertThatThrownBy(() -> service.batchDelete(java.util.Arrays.asList("a", null)))
                    .isInstanceOf(BizException.class).hasMessage("缓存键不能为空");
        }

        @Test
        @DisplayName("非正 TTL 统一抛 BizException（不再由各后端抛原始异常）")
        void rejectsNonPositiveTtl() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.put("k", "v", Duration.ZERO))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
            assertThatThrownBy(() -> service.put("k", "v", Duration.ofSeconds(-5)))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
            assertThatThrownBy(() -> service.putIfAbsent("k", "v", Duration.ZERO))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
            assertThatThrownBy(() -> service.expire("k", Duration.ofSeconds(-1)))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
            assertThatThrownBy(() -> service.increment("k", Duration.ZERO))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL必须大于 0");
        }

        @Test
        @DisplayName("TTL 参数为 null 时抛 BizException（expire/increment 必填）")
        void rejectsNullTtl() {
            FakeCacheService service = service("");

            assertThatThrownBy(() -> service.expire("k", null))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL不能为空");
            assertThatThrownBy(() -> service.increment("k", null))
                    .isInstanceOf(BizException.class).hasMessage("缓存TTL不能为空");
        }
    }

    @Nested
    @DisplayName("其余操作")
    class Operations {

        @Test
        @DisplayName("increment 走后端实现并返回自增后的值")
        void increment() {
            FakeCacheService service = service("");

            assertThat(service.increment("counter", Duration.ofMinutes(1))).isEqualTo(1L);
            assertThat(service.increment("counter", Duration.ofMinutes(1))).isEqualTo(2L);
        }

        @Test
        @DisplayName("getExpire 透传后端剩余秒数（缺失时 -2）")
        void getExpire() {
            FakeCacheService service = service("");
            service.put("k", "v");

            assertThat(service.getExpire("k")).isEqualTo(60L);
            assertThat(service.getExpire("absent")).isEqualTo(-2L);
        }

        @Test
        @DisplayName("putIfAbsent 首次成功、再次返回 false")
        void putIfAbsent() {
            FakeCacheService service = service("");

            assertThat(service.putIfAbsent("k", "v")).isTrue();
            assertThat(service.putIfAbsent("k", "v2")).isFalse();
            // 落盘是 JSON 字符串（带引号），读取按传入类型还原为 "v"
            assertThat(service.store.get("k")).isEqualTo("\"v\"");
            assertThat(service.get("k", String.class)).isEqualTo("v");
        }

        @Test
        @DisplayName("batchDelete 返回实际删除数量")
        void batchDelete() {
            FakeCacheService service = service("");
            service.put("a", "1");
            service.put("b", "2");

            assertThat(service.batchDelete(List.of("a", "b", "c"))).isEqualTo(2L);
        }
    }
}
