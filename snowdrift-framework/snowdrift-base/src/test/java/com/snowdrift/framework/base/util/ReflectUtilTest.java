package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ReflectUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 反射工具类单元测试
 * @since 1.0.0
 */
class ReflectUtilTest {

    @SuppressWarnings("unused")
    static class Parent {
        private static final String PARENT_STATIC = "static";
        private String b = "b";
        private String shared = "parent-shared";

        public String getB() {
            return b;
        }
    }

    @SuppressWarnings("unused")
    static class Child extends Parent {
        private String a = "a";
        private String shared = "child-shared";
        private String c = "c";

        public String getA() {
            return a;
        }

        public String overloaded(int value) {
            return "int:" + value;
        }

        public String overloaded(String value) {
            return "string:" + value;
        }

        private String secret() {
            return "secret";
        }
    }

    private static List<String> fieldNames(List<Field> fields) {
        return fields.stream().map(Field::getName).toList();
    }

    @Nested
    @DisplayName("getDeclaredFields")
    class Fields {

        @Test
        @DisplayName("默认只取当前类，排除 static 字段")
        void currentClassOnly() {
            List<String> names = fieldNames(ReflectUtil.getDeclaredFields(new Child()));

            assertThat(names).containsExactlyInAnyOrder("a", "shared", "c");
            assertThat(names).doesNotContain("PARENT_STATIC", "b");
        }

        @Test
        @DisplayName("包含父类时子类优先按名去重")
        void includesSuperclassWithChildPriority() {
            List<String> names = fieldNames(ReflectUtil.getDeclaredFields(new Child(), true));

            assertThat(names).containsExactly("a", "shared", "c", "b");
            // 同名字段只保留子类声明的那一个
            assertThat(names.stream().filter("shared"::equals)).hasSize(1);
        }

        @Test
        @DisplayName("接受 Class 与实例两种入参，结果一致")
        void acceptsClassOrInstance() {
            assertThat(fieldNames(ReflectUtil.getDeclaredFields(Child.class, true)))
                    .isEqualTo(fieldNames(ReflectUtil.getDeclaredFields(new Child(), true)));
            assertThat(fieldNames(ReflectUtil.getDeclaredFields(Parent.class)))
                    .containsExactlyInAnyOrder("b", "shared");
        }

        @Test
        @DisplayName("null 返回空列表而不是抛异常")
        void nullReturnsEmptyList() {
            assertThat(ReflectUtil.getDeclaredFields((Object) null)).isEmpty();
            assertThat(ReflectUtil.getDeclaredFields((Object) null, true)).isEmpty();
        }

        @Test
        @DisplayName("返回的字段可直接读取（非 public 字段由调用方 setAccessible）")
        void returnedFieldsAreUsableAfterSetAccessible() throws Exception {
            Child child = new Child();
            List<Field> fields = ReflectUtil.getDeclaredFields(child);

            for (Field field : fields) {
                field.setAccessible(true);
            }

            assertThat(fields).hasSize(3);
            assertThat(fields.stream().filter(f -> "a".equals(f.getName())).findFirst().orElseThrow().get(child))
                    .isEqualTo("a");
        }
    }

    @Nested
    @DisplayName("getMethod")
    class Methods {

        @Test
        @DisplayName("按名称查找当前类的声明方法并设置可访问")
        void byName() throws Exception {
            Method method = ReflectUtil.getMethod(Child.class, "getA");

            assertThat(method.getName()).isEqualTo("getA");
            assertThat(method.canAccess(new Child())).isTrue();
            assertThat(method.invoke(new Child())).isEqualTo("a");
        }

        @Test
        @DisplayName("私有方法同样可访问")
        void privateMethod() throws Exception {
            Method method = ReflectUtil.getMethod(Child.class, "secret");

            method.setAccessible(true);
            assertThat(method.invoke(new Child())).isEqualTo("secret");
        }

        @Test
        @DisplayName("重载方法返回其中之一（JVM 不保证顺序），提示调用方使用精确重载")
        void overloadedReturnsFirstMatch() {
            Method method = ReflectUtil.getMethod(Child.class, "overloaded");

            assertThat(method.getName()).isEqualTo("overloaded");
            assertThat(method.getParameterCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("按参数类型精确查找")
        void byParameterTypes() throws Exception {
            Method intOverload = ReflectUtil.getMethod(Child.class, "overloaded", int.class);
            assertThat(intOverload.invoke(new Child(), 42)).isEqualTo("int:42");

            Method stringOverload = ReflectUtil.getMethod(Child.class, "overloaded", String.class);
            assertThat(stringOverload.invoke(new Child(), "x")).isEqualTo("string:x");

            Method noArgs = ReflectUtil.getMethod(Child.class, "getA", new Class<?>[0]);
            assertThat(noArgs.invoke(new Child())).isEqualTo("a");
        }

        @Test
        @DisplayName("只搜索当前类，不搜索父类")
        void doesNotSearchSuperclass() {
            // getB 声明在 Parent 上
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, "getB"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("方法不存在")
                    .hasMessageContaining("getB");
            assertThat(ReflectUtil.getMethod(Parent.class, "getB").getName()).isEqualTo("getB");
        }

        @Test
        @DisplayName("方法不存在或参数类型不匹配时抛 BizException")
        void missingMethods() {
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, "missing"))
                    .isInstanceOf(BizException.class).hasMessageContaining("方法不存在");
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, "overloaded", double.class))
                    .isInstanceOf(BizException.class).hasMessageContaining("无法获取方法");
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, "getA", String.class))
                    .isInstanceOf(BizException.class).hasMessageContaining("无法获取方法");
        }

        @Test
        @DisplayName("入参校验")
        void argumentValidation() {
            assertThatThrownBy(() -> ReflectUtil.getMethod(null, "getA"))
                    .isInstanceOf(BizException.class).hasMessage("目标类型不能为空");
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, " "))
                    .isInstanceOf(BizException.class).hasMessage("方法名不能为空");
            assertThatThrownBy(() -> ReflectUtil.getMethod(Child.class, "getA", (Class<?>[]) null))
                    .isInstanceOf(BizException.class).hasMessage("参数类型不能为空");
        }
    }
}
