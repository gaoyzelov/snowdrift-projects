package com.snowdrift.framework.cache.handler;

import com.snowdrift.framework.base.util.EncryptUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftKeyGeneratorTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 统一缓存 Key 生成器单元测试
 * @since 1.0.0
 */
class SnowdriftKeyGeneratorTest {

    private static final String TYPE = SnowdriftKeyGeneratorTest.Target.class.getName();

    private final SnowdriftKeyGenerator generator = new SnowdriftKeyGenerator();

    private static Method method(String name, Class<?>... types) throws Exception {
        return Target.class.getDeclaredMethod(name, types);
    }

    @SuppressWarnings("unused")
    static class Target {
        void noArgs() {
        }

        void one(String name) {
        }

        void two(String a, String b) {
        }

        void pojo(Payload payload) {
        }

        void overloaded(Integer value) {
        }

        void overloaded(Long value) {
        }

        void overloaded(String value) {
        }

        void enumArg(Level level) {
        }
    }

    public enum Level {
        HIGH
    }

    public static class Payload {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    @DisplayName("无参数：完整类名#方法名（完整类名可避免不同包同名类互相撞 key）")
    void noArgs() throws Exception {
        assertThat(generator.generate(new Target(), method("noArgs"))).isEqualTo(TYPE + "#noArgs");
        assertThat(TYPE).contains(".").doesNotContain("#");
    }

    @Test
    @DisplayName("参数格式为「类型:值」，简单类型值直接用 toString")
    void simpleParams() throws Exception {
        assertThat(generator.generate(new Target(), method("one", String.class), "abc"))
                .isEqualTo(TYPE + "#one:java.lang.String:abc");
        assertThat(generator.generate(new Target(), method("enumArg", Level.class), Level.HIGH))
                .isEqualTo(TYPE + "#enumArg:" + Level.class.getName() + ":HIGH");
    }

    @Test
    @DisplayName("回归：同名重载方法不再撞 key（key 携带参数类型）")
    void overloadsDoNotCollide() throws Exception {
        Object integerKey = generator.generate(new Target(), method("overloaded", Integer.class), 1);
        Object longKey = generator.generate(new Target(), method("overloaded", Long.class), 1L);
        Object stringKey = generator.generate(new Target(), method("overloaded", String.class), "1");

        assertThat(List.of(integerKey, longKey, stringKey)).doesNotHaveDuplicates();
        assertThat(integerKey).isEqualTo(TYPE + "#overloaded:java.lang.Integer:1");
        assertThat(longKey).isEqualTo(TYPE + "#overloaded:java.lang.Long:1");
        assertThat(stringKey).isEqualTo(TYPE + "#overloaded:java.lang.String:1");
    }

    @Test
    @DisplayName("复杂对象使用字段序列化（不依赖 toString，跨实例稳定）")
    void complexParams() throws Exception {
        Payload first = new Payload();
        first.setName("a");
        first.setValue(1);
        Payload second = new Payload();
        second.setName("a");
        second.setValue(1);

        Object key1 = generator.generate(new Target(), method("pojo", Payload.class), first);
        Object key2 = generator.generate(new Target(), method("pojo", Payload.class), second);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.toString()).startsWith(TYPE + "#pojo:" + Payload.class.getName() + ":").contains("\"name\"");
    }

    @Test
    @DisplayName("超长参数取 sha256: 前缀摘要；且与「值恰为摘要字串」的参数不再撞 key")
    void longParamIsHashedWithoutColliding() throws Exception {
        String longName = "x".repeat(200);
        String digest = EncryptUtil.sha256(longName);

        Object hashedKey = generator.generate(new Target(), method("one", String.class), longName);
        Object literalDigestKey = generator.generate(new Target(), method("one", String.class), digest);

        assertThat(hashedKey.toString()).contains("sha256");
        assertThat(hashedKey.toString()).hasSize((TYPE + "#one:java.lang.String:").length() + "sha256\\:".length() + 64);
        assertThat(hashedKey).isNotEqualTo(literalDigestKey);
    }

    @Test
    @DisplayName("恰好 64 字符的参数原样保留，不摘要")
    void boundaryLengthIsKept() throws Exception {
        String exactly64 = "y".repeat(64);

        assertThat(generator.generate(new Target(), method("one", String.class), exactly64))
                .isEqualTo(TYPE + "#one:java.lang.String:" + exactly64);
    }

    @Test
    @DisplayName("分隔符转义：m(\"a:b\") 与 m(\"a\",\"b\") 不会撞成同一个 key")
    void escapesDelimiters() throws Exception {
        Object single = generator.generate(new Target(), method("one", String.class), "a:b");
        Object two = generator.generate(new Target(), method("two", String.class, String.class), "a", "b");

        assertThat(single).isEqualTo(TYPE + "#one:java.lang.String:a\\:b");
        assertThat(two).isEqualTo(TYPE + "#two:java.lang.String:a:java.lang.String:b");
        assertThat(single).isNotEqualTo(two);
    }

    @Test
    @DisplayName("反斜杠先转义，保证结果可逆")
    void escapesBackslash() throws Exception {
        assertThat(generator.generate(new Target(), method("one", String.class), "a\\:b"))
                .isEqualTo(TYPE + "#one:java.lang.String:a\\\\\\:b");
    }

    @Test
    @DisplayName("已知歧义：同样声明类型下 null 与空串生成相同片段（javadoc 已声明）")
    void nullAndEmptyCollide() throws Exception {
        Object nullArg = generator.generate(new Target(), method("one", String.class), (Object) null);
        Object emptyArg = generator.generate(new Target(), method("one", String.class), "");

        assertThat(nullArg).isEqualTo(emptyArg).isEqualTo(TYPE + "#one:java.lang.String:");
    }

    @Test
    @DisplayName("不同方法/不同参数得到不同 key")
    void distinctKeys() throws Exception {
        Object a = generator.generate(new Target(), method("one", String.class), "x");
        Object b = generator.generate(new Target(), method("two", String.class, String.class), "x", "y");
        Object c = generator.generate(new Target(), method("one", String.class), "y");

        assertThat(List.of(a, b, c)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("参数为 Map/List 等容器时按字段序列化（内容不同则 key 不同）")
    void containersDiffer() throws Exception {
        Object map1 = generator.generate(new Target(), method("one", String.class), Map.of("a", 1));
        Object map2 = generator.generate(new Target(), method("one", String.class), Map.of("a", 2));

        assertThat(map1).isNotEqualTo(map2);
    }
}
