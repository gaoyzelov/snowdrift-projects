package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CacheSerializerTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 缓存序列化器（Jackson）单元测试
 * @since 1.0.0
 */
class CacheSerializerTest {

    /** 私有字段 + getter/setter */
    public static class User {
        private Long id;
        private String name;
        private LocalDateTime createdAt;

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

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 只有 getter、没有 setter——序列化侧必须走字段（否则读回来是默认值）。
     * <p>字段初始值刻意与写入值不同：若读侧没有按字段还原，断言会立刻暴露。</p>
     */
    public static class GetterOnly {
        private String no = "DEFAULT-no";
        private int amount = -1;

        public String getNo() {
            return no;
        }

        public int getAmount() {
            return amount;
        }
    }

    /** 只有 public 字段 */
    public static class PublicField {
        public String no = "DEFAULT-no";
    }

    /** 序列化必然失败的样本：getter 抛异常 */
    public static class BrokenGetter {
        public String getValue() {
            throw new IllegalStateException("getter 故意抛错");
        }
    }

    /** 自引用对象：序列化器需给出可控异常，而不是让调用方拿到 StackOverflowError */
    public static class Cyclic {
        private Cyclic self = this;

        public Cyclic getSelf() {
            return self;
        }

        public void setSelf(Cyclic self) {
            this.self = self;
        }
    }

    /** 反射写字段：用于构造 setter-less 样本 */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        user.setCreatedAt(LocalDateTime.of(2026, 10, 9, 12, 0, 0));
        return user;
    }

    /** 两个实现共同满足的契约 */
    private static void assertCommonContract(ICacheSerializer serializer) throws Exception {
        // 1. 常规 POJO 往返（含 java.time 与中文）
        User back = serializer.deserialize(serializer.serialize(user()), User.class);
        assertThat(back.getId()).isEqualTo(1L);
        assertThat(back.getName()).isEqualTo("张三");
        assertThat(back.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 9, 12, 0, 0));

        // 2. setter-less 类：字段值必须真正还原，而不是保留字段默认值
        GetterOnly getterOnly = new GetterOnly();
        setField(getterOnly, "no", "SN-123");
        setField(getterOnly, "amount", 999);
        GetterOnly restored = serializer.deserialize(serializer.serialize(getterOnly), GetterOnly.class);
        assertThat(restored.getNo()).isEqualTo("SN-123");
        assertThat(restored.getAmount()).isEqualTo(999);

        // 3. 只有 public 字段的类也能写回
        PublicField publicField = new PublicField();
        publicField.no = "PF-1";
        assertThat(serializer.deserialize(serializer.serialize(publicField), PublicField.class).no).isEqualTo("PF-1");

        // 4. null 值处理：序列化与反序列化均为 null
        assertThat(serializer.serialize(null)).isNull();
        assertThat(serializer.deserialize(null, User.class)).isNull();

        // 5. 未知字段容忍（结构演进后仍可读旧数据）
        assertThat(serializer.deserialize("{\"id\":5,\"unknownField\":\"x\"}", User.class).getId()).isEqualTo(5L);

        // 6. 空 JSON 对象 → 全 null 对象，不抛异常
        assertThat(serializer.deserialize("{}", User.class).getId()).isNull();

        // 7. 非法 JSON → BizException（统一异常口径）
        assertThatThrownBy(() -> serializer.deserialize("{not-json", User.class))
                .isInstanceOf(BizException.class).hasMessage("缓存反序列化失败");

        // 8. 自引用对象 → 可预期的业务异常，而不是 StackOverflowError 逃逸
        assertThatThrownBy(() -> serializer.serialize(new Cyclic()))
                .isInstanceOf(BizException.class).hasMessage("缓存序列化失败");

        // 9. 集合字段：元素按 Map 读回（接口只接受 Class<T>，无法表达元素类型）
        Object listBack = serializer.deserialize(serializer.serialize(List.of(user())), List.class);
        assertThat(listBack).isInstanceOf(List.class);
        assertThat(((List<?>) listBack).get(0)).isNotInstanceOf(User.class);

        // 10. Map 中的 null 值会被丢弃（两个实现都是非空序列化策略）
        Map<String, Object> withNull = new java.util.LinkedHashMap<>();
        withNull.put("present", "v");
        withNull.put("absent", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> mapBack = (Map<String, Object>) serializer.deserialize(serializer.serialize(withNull), Map.class);
        assertThat(mapBack).containsEntry("present", "v").doesNotContainKey("absent");
    }

    @Nested
    @DisplayName("JacksonCacheSerializer")
    class Jackson {

        private final ICacheSerializer serializer = new JacksonCacheSerializer();

        @Test
        @DisplayName("满足缓存序列化器共同契约")
        void contract() throws Exception {
            assertCommonContract(serializer);
        }

        @Test
        @DisplayName("不写入类型元数据（读取依赖调用方传入的 Class）")
        void noTypeMetadata() {
            assertThat(serializer.serialize(user())).doesNotContain("@class");
        }

        @Test
        @DisplayName("null 字段不序列化（NON_NULL）")
        void nullFieldsOmitted() {
            User user = new User();
            user.setId(1L);

            assertThat(serializer.serialize(user)).isEqualTo("{\"id\":1}");
        }

        @Test
        @DisplayName("时间按 ISO-8601 字符串落盘（LocalDateTime → 2026-10-09T12:00:00）")
        void datesUseIsoFormat() {
            assertThat(serializer.serialize(user())).contains("\"createdAt\":\"2026-10-09T12:00:00\"");
        }

        @Test
        @DisplayName("getter 抛异常时包装为 BizException（Jackson 按 getter 取值）")
        void serializationFailureIsWrapped() {
            assertThatThrownBy(() -> serializer.serialize(new BrokenGetter()))
                    .isInstanceOf(BizException.class).hasMessage("缓存序列化失败");
        }

        @Test
        @DisplayName("泛型 Type 重载：集合元素还原为目标类型（List<User> / Map<String,User>）")
        void typeOverloadRestoresGenerics() {
            List<User> users = List.of(user());
            Type listType = new TypeReference<List<User>>() {
            }.getType();
            Type mapType = new TypeReference<Map<String, User>>() {
            }.getType();

            Object listBack = serializer.deserialize(serializer.serialize(users), listType);
            assertThat(listBack).isInstanceOf(List.class);
            assertThat(((List<?>) listBack).get(0)).isInstanceOf(User.class);
            assertThat(((User) ((List<?>) listBack).get(0)).getName()).isEqualTo("张三");

            Object mapBack = serializer.deserialize(serializer.serialize(Map.of("u", users.get(0))), mapType);
            assertThat(((Map<?, ?>) mapBack).get("u")).isInstanceOf(User.class);
        }

        @Test
        @DisplayName("cacheValueMapper 为 Spring Cache 场景开启多态类型信息")
        void cacheValueMapperEnablesTyping() throws Exception {
            com.fasterxml.jackson.databind.ObjectMapper mapper = JacksonCacheSerializer.cacheValueMapper();

            String json = mapper.writeValueAsString(List.of(user()));
            assertThat(json).contains("@class");

            Object back = mapper.readValue(json, Object.class);
            assertThat(back).isInstanceOf(List.class);
            assertThat(((List<?>) back).get(0)).isInstanceOf(User.class);
        }
    }
}
