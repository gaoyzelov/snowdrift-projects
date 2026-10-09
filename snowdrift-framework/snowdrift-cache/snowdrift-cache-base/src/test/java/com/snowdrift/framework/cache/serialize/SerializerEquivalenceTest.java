package com.snowdrift.framework.cache.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SerializerEquivalenceTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 两个缓存序列化器的等价性验收：同一对象产出同一 JSON，且数据可互相读取
 * @since 1.0.0
 */
class SerializerEquivalenceTest {

    private final ICacheSerializer jackson = new JacksonCacheSerializer();
    private final ICacheSerializer fastjson2 = new FastJson2CacheSerializer();

    /** 私有字段 + getter/setter，且实现 equals 以便交叉读取后比对 */
    public static class Order {
        private Long id;
        private String name;
        private LocalDateTime createdAt;

        public Order() {
        }

        public Order(Long id, String name, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }

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

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Order other)) {
                return false;
            }
            return java.util.Objects.equals(id, other.id)
                    && java.util.Objects.equals(name, other.name)
                    && java.util.Objects.equals(createdAt, other.createdAt);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, createdAt);
        }
    }

    /** 只有 getter、没有 setter（写读都必须走字段） */
    public static class SetterLess {
        private String no = "DEFAULT-no";
        private int amount = -1;

        public String getNo() {
            return no;
        }

        public int getAmount() {
            return amount;
        }
    }

    /** getter 无对应字段的计算属性：两边都不应写出 */
    public static class Computed {
        private String name = "n";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplay() {
            return "computed:" + name;
        }
    }

    /** 私有字段且无任何访问器：两边都应按字段写出 */
    public static class Hidden {
        private String secret = "hidden";
        public String marker = "pub";
    }

    /** 各时间类型与 java.util.Date */
    public static class Times {
        private LocalDateTime dateTime = LocalDateTime.of(2026, 10, 9, 12, 0, 0);
        private LocalDate date = LocalDate.of(2026, 10, 9);
        private LocalTime time = LocalTime.of(12, 0, 0);
        private OffsetDateTime offsetDateTime = OffsetDateTime.of(2026, 10, 9, 12, 0, 0, 0, ZoneOffset.ofHours(8));
        private Instant instant = Instant.ofEpochSecond(1_700_000_000L);
        private Date utilDate = new Date(1_700_000_000_000L);
        private java.sql.Timestamp timestamp = new java.sql.Timestamp(1_700_000_000_000L);
        private long millis = 1_700_000_000_000L;
        private Long bigNumber = 9_007_199_254_740_993L;
    }

    public record Rec(Long id, String name) {
    }

    public static class Nulls {
        private String absent = null;
        private String present = "v";
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static SetterLess setterLess() throws Exception {
        SetterLess value = new SetterLess();
        setField(value, "no", "SN-123");
        setField(value, "amount", 999);
        return value;
    }

    /** 逐字比对：两个序列化器对同一对象必须产出完全相同的 JSON */
    private void assertSameJson(String label, Object value) {
        String jacksonJson = jackson.serialize(value);
        String fastjson2Json = fastjson2.serialize(value);

        assertThat(jacksonJson).as("样本[%s] 两个序列化器输出应逐字一致", label).isEqualTo(fastjson2Json);
    }

    @Test
    @DisplayName("字段口径一致：普通 POJO / setter-less / 计算属性 / 无访问器私有字段 / record")
    void sameJsonForObjectShapes() throws Exception {
        assertSameJson("普通 POJO", new Order(1L, "张三", LocalDateTime.of(2026, 10, 9, 12, 0, 0)));
        assertSameJson("setter-less", setterLess());
        assertSameJson("计算属性", new Computed());
        assertSameJson("无访问器私有字段", new Hidden());
        assertSameJson("record", new Rec(1L, "r"));

        // 计算属性与无访问器字段的口径确认（两边同为字段口径）
        assertThat(jackson.serialize(new Computed())).doesNotContain("display");
        assertThat(fastjson2.serialize(new Computed())).doesNotContain("display");
        assertThat(jackson.serialize(new Hidden())).contains("secret");
        assertThat(fastjson2.serialize(new Hidden())).contains("secret");
    }

    @Test
    @DisplayName("日期时间格式一致：LocalDateTime/Date/Timestamp 为空格格式，其余为 ISO 风格")
    void sameJsonForTimeTypes() {
        String json = jackson.serialize(new Times());
        assertThat(json).isEqualTo(fastjson2.serialize(new Times()));

        assertThat(json).contains("\"dateTime\":\"2026-10-09 12:00:00\"");
        assertThat(json).doesNotContain("2026-10-09T12:00:00\"");
        assertThat(json).contains("\"date\":\"2026-10-09\"", "\"time\":\"12:00:00\"");
        assertThat(json).contains("\"offsetDateTime\":\"2026-10-09T12:00:00+08:00\"");
        assertThat(json).contains("\"instant\":\"2023-11-14T22:13:20Z\"");
        // java.util.Date 与 sql.Timestamp 按 JVM 默认时区输出同一格式
        assertThat(json).contains("\"utilDate\":\"").contains("\"timestamp\":\"");
    }

    @Test
    @DisplayName("容器、null 与数值一致：嵌套集合、Map 的空值、大整数、中文")
    void sameJsonForContainersAndNulls() {
        Map<String, Object> withNull = new LinkedHashMap<>();
        withNull.put("present", "v");
        withNull.put("absent", null);

        assertSameJson("null 字段", new Nulls());
        assertSameJson("Map 含 null 值", withNull);
        assertSameJson("嵌套集合", List.of(new Rec(1L, "a"), Map.of("k", "v")));
        assertSameJson("中文与大整数", Map.of("名称", "张三", "big", 9_007_199_254_740_993L));

        // 两边都不写类型元数据（ICacheService 路径）
        assertThat(jackson.serialize(new Order(1L, "a", null))).doesNotContain("@class");
        assertThat(fastjson2.serialize(new Order(1L, "a", null))).doesNotContain("@type");
    }

    @Test
    @DisplayName("Map 键序确定：内容相同的 HashMap 无论插入顺序如何，两个序列化器都产出同一 JSON")
    void mapOrderIsDeterministic() {
        java.util.HashMap<String, String> first = new java.util.HashMap<>();
        first.put("b", "2");
        first.put("a", "1");
        first.put("c", "3");
        java.util.HashMap<String, String> second = new java.util.HashMap<>();
        second.put("c", "3");
        second.put("a", "1");
        second.put("b", "2");

        assertThat(jackson.serialize(first)).isEqualTo(jackson.serialize(second));
        assertThat(fastjson2.serialize(first)).isEqualTo(fastjson2.serialize(second));
        assertThat(jackson.serialize(first)).isEqualTo(fastjson2.serialize(first));
    }

    @Test
    @DisplayName("交叉互读：任一序列化器写出的数据，另一个都能读回同样的对象")
    void crossReadable() {
        Order source = new Order(7L, "李四", LocalDateTime.of(2026, 10, 9, 12, 0, 0));

        String byJackson = jackson.serialize(source);
        String byFastjson2 = fastjson2.serialize(source);

        assertThat(jackson.deserialize(byFastjson2, Order.class)).isEqualTo(source);
        assertThat(fastjson2.deserialize(byJackson, Order.class)).isEqualTo(source);
        assertThat(fastjson2.deserialize(byFastjson2, Order.class)).isEqualTo(source);
        assertThat(jackson.deserialize(byJackson, Order.class)).isEqualTo(source);
    }

    @Test
    @DisplayName("交叉互读：旧格式（ISO 时间）仍可被两者读取")
    void crossReadableForIsoLegacyText() {
        String isoJson = "{\"createdAt\":\"2026-10-09T12:00:00\",\"id\":1,\"name\":\"张三\"}";

        Order expected = new Order(1L, "张三", LocalDateTime.of(2026, 10, 9, 12, 0, 0));
        assertThat(jackson.deserialize(isoJson, Order.class)).isEqualTo(expected);
        assertThat(fastjson2.deserialize(isoJson, Order.class)).isEqualTo(expected);
    }

    @Test
    @DisplayName("泛型重载：两侧都能把集合元素还原为目标类型（List<Order> / Map<String,Order>）")
    void typeOverloadRestoresGenerics() {
        List<Order> orders = List.of(new Order(1L, "a", LocalDateTime.of(2026, 10, 9, 12, 0, 0)));
        Type listType = new TypeReference<List<Order>>() {
        }.getType();
        Type mapType = new TypeReference<Map<String, Order>>() {
        }.getType();

        for (ICacheSerializer serializer : List.of(jackson, fastjson2)) {
            String name = serializer.getClass().getSimpleName();

            Object listBack = serializer.deserialize(serializer.serialize(orders), listType);
            assertThat(listBack).as(name).isInstanceOf(List.class);
            assertThat(((List<?>) listBack).get(0)).as(name).isInstanceOf(Order.class);
            assertThat(((List<?>) listBack).get(0)).as(name).isEqualTo(orders.get(0));

            Object mapBack = serializer.deserialize(serializer.serialize(Map.of("o", orders.get(0))), mapType);
            assertThat(((Map<?, ?>) mapBack).get("o")).as(name).isInstanceOf(Order.class);
        }
    }

    @Test
    @DisplayName("两个序列化器都拒绝 null 输入（序列化返回 null、反序列化返回 null）")
    void nullContract() {
        for (ICacheSerializer serializer : List.of(jackson, fastjson2)) {
            assertThat(serializer.serialize(null)).isNull();
            assertThat(serializer.deserialize(null, Order.class)).isNull();

            Object nullForType = serializer.deserialize(null, new TypeReference<List<Order>>() {
            }.getType());
            assertThat(nullForType).isNull();
        }
    }
}
