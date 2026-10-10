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

    private static final String TYPE = SnowdriftKeyGeneratorTest.Target.class.getSimpleName();

    private final SnowdriftKeyGenerator generator = new SnowdriftKeyGenerator();

    private static Method method(String name, Class<?>... types) throws Exception {
        return Target.class.getDeclaredMethod(name, types);
    }

    /** 独立重建摘要口径：参数段（类型:值，值转义后）整体取 SHA-256 前 128 位 */
    private static String digestOf(String section) {
        return EncryptUtil.sha256(section).substring(0, 32);
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
    @DisplayName("无参数：简单类名#方法名（带参数才摘要）")
    void noArgs() throws Exception {
        assertThat(generator.generate(new Target(), method("noArgs"))).isEqualTo(TYPE + "#noArgs");
        assertThat(TYPE).isEqualTo("Target").doesNotContain(".");
    }

    @Test
    @DisplayName("带参数：参数段「类型:值」整体摘要（32 位十六进制），明文值不进入 key")
    void paramsAreDigested() throws Exception {
        Object key = generator.generate(new Target(), method("one", String.class), "abc");

        assertThat(key).isEqualTo(TYPE + "#one:" + digestOf("String:abc"));
        assertThat(key.toString()).hasSize(TYPE.length() + "#one:".length() + 32);
        assertThat(generator.generate(new Target(), method("enumArg", Level.class), Level.HIGH))
                .isEqualTo(TYPE + "#enumArg:" + digestOf("Level:HIGH"));
    }

    @Test
    @DisplayName("回归：同名重载方法不再撞 key（类型参与摘要输入）")
    void overloadsDoNotCollide() throws Exception {
        Object integerKey = generator.generate(new Target(), method("overloaded", Integer.class), 1);
        Object longKey = generator.generate(new Target(), method("overloaded", Long.class), 1L);
        Object stringKey = generator.generate(new Target(), method("overloaded", String.class), "1");

        assertThat(List.of(integerKey, longKey, stringKey)).doesNotHaveDuplicates();
        assertThat(integerKey).isEqualTo(TYPE + "#overloaded:" + digestOf("Integer:1"));
        assertThat(longKey).isEqualTo(TYPE + "#overloaded:" + digestOf("Long:1"));
        assertThat(stringKey).isEqualTo(TYPE + "#overloaded:" + digestOf("String:1"));
    }

    @Test
    @DisplayName("复杂对象按字段序列化后参与摘要（跨实例稳定、内容不同则 key 不同）")
    void complexParams() throws Exception {
        Payload first = new Payload();
        first.setName("a");
        first.setValue(1);
        Payload second = new Payload();
        second.setName("a");
        second.setValue(1);
        Payload other = new Payload();
        other.setName("a");
        other.setValue(2);

        Object key1 = generator.generate(new Target(), method("pojo", Payload.class), first);
        Object key2 = generator.generate(new Target(), method("pojo", Payload.class), second);
        Object key3 = generator.generate(new Target(), method("pojo", Payload.class), other);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key1.toString()).startsWith(TYPE + "#pojo:").hasSize(TYPE.length() + "#pojo:".length() + 32);
    }

    @Test
    @DisplayName("参数长度不做限制：任意长度参数 key 长度恒定（32 位摘要）")
    void keyLengthIsConstant() throws Exception {
        String shortValue = "x".repeat(10);
        String hugeValue = "x".repeat(5000);

        Object shortKey = generator.generate(new Target(), method("one", String.class), shortValue);
        Object hugeKey = generator.generate(new Target(), method("one", String.class), hugeValue);

        assertThat(shortKey.toString()).hasSize(TYPE.length() + "#one:".length() + 32);
        assertThat(hugeKey.toString()).hasSize(TYPE.length() + "#one:".length() + 32);
        assertThat(hugeKey).isEqualTo(TYPE + "#one:" + digestOf("String:" + hugeValue));
        assertThat(shortKey).isNotEqualTo(hugeKey);
    }

    @Test
    @DisplayName("多参数按位置参与摘要：(\"a\", null) 与 (null, \"a\") 得到不同 key")
    void nullPositionMatters() throws Exception {
        Object firstNull = generator.generate(new Target(), method("two", String.class, String.class), "a", null);
        Object secondNull = generator.generate(new Target(), method("two", String.class, String.class), null, "a");

        assertThat(firstNull).isEqualTo(TYPE + "#two:" + digestOf("String:a:String:"));
        // (null, "a")：null 的空片段自身带一个冒号，再接分隔符，故为 String::String:a
        assertThat(secondNull).isEqualTo(TYPE + "#two:" + digestOf("String::String:a"));
        assertThat(firstNull).isNotEqualTo(secondNull);
    }

    @Test
    @DisplayName("分隔符转义参与摘要输入：m(\"a:String:b\") 与 m(\"a\",\"b\") 不会摘要出相同 key")
    void escapesDelimiters() throws Exception {
        Object single = generator.generate(new Target(), method("one", String.class), "a:String:b");
        Object two = generator.generate(new Target(), method("two", String.class, String.class), "a", "b");

        assertThat(single).isEqualTo(TYPE + "#one:" + digestOf("String:a\\:String\\:b"));
        assertThat(two).isEqualTo(TYPE + "#two:" + digestOf("String:a:String:b"));
        assertThat(single).isNotEqualTo(two);
    }

    @Test
    @DisplayName("反斜杠先转义，保证摘要输入可逆")
    void escapesBackslash() throws Exception {
        assertThat(generator.generate(new Target(), method("one", String.class), "a\\:b"))
                .isEqualTo(TYPE + "#one:" + digestOf("String:a\\\\\\:b"));
    }

    @Test
    @DisplayName("已知歧义：同样声明类型下 null 与空串摘要输入相同（javadoc 已声明）")
    void nullAndEmptyCollide() throws Exception {
        Object nullArg = generator.generate(new Target(), method("one", String.class), (Object) null);
        Object emptyArg = generator.generate(new Target(), method("one", String.class), "");

        assertThat(nullArg).isEqualTo(emptyArg).isEqualTo(TYPE + "#one:" + digestOf("String:"));
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
