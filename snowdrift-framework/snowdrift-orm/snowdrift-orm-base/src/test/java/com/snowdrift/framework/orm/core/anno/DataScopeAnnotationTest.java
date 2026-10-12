package com.snowdrift.framework.orm.core.anno;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeAnnotationTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 数据权限注解黑盒测试：默认字段名（dept_id/user_id）、自定义列、类与方法的挂载与
 * 方法优先契约、RUNTIME 保留、仅 TYPE+METHOD 生效且不被子类继承
 * @since 1.0.0
 */
class DataScopeAnnotationTest {

    @DataScope
    static class DefaultMapper {
    }

    @DataScope(alias = "u", deptColumn = "d.dept_id", userColumn = "u.id")
    static class CustomMapper {

        @DataScope(alias = "o")
        public void selectByMethod() {
        }
    }

    static class InheritedFromAnnotated extends DefaultMapper {
    }

    @Test
    @DisplayName("默认值：alias 为空串，部门列 dept_id、用户列 user_id")
    void defaults() {
        DataScope dataScope = DefaultMapper.class.getAnnotation(DataScope.class);

        assertThat(dataScope).isNotNull();
        assertThat(dataScope.alias()).isEmpty();
        assertThat(dataScope.deptColumn()).isEqualTo("dept_id");
        assertThat(dataScope.userColumn()).isEqualTo("user_id");
    }

    @Test
    @DisplayName("自定义值：类上可覆盖三列，方法上的注解可独立取值（方法优先的前提）")
    void customValuesAndMethodLevel() throws Exception {
        DataScope onType = CustomMapper.class.getAnnotation(DataScope.class);
        assertThat(onType.alias()).isEqualTo("u");
        assertThat(onType.deptColumn()).isEqualTo("d.dept_id");
        assertThat(onType.userColumn()).isEqualTo("u.id");

        Method method = CustomMapper.class.getMethod("selectByMethod");
        DataScope onMethod = method.getAnnotation(DataScope.class);
        assertThat(onMethod).isNotNull();
        assertThat(onMethod.alias()).isEqualTo("o");
        // 方法未声明的属性回落到注解默认值，而非类上的值
        assertThat(onMethod.deptColumn()).isEqualTo("dept_id");
        assertThat(onMethod.userColumn()).isEqualTo("user_id");
    }

    @Test
    @DisplayName("元注解契约：RUNTIME 保留，且仅可标注在 TYPE 与 METHOD 上（不可标注字段）")
    void retentionAndTarget() {
        Retention retention = DataScope.class.getAnnotation(Retention.class);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);

        Target target = DataScope.class.getAnnotation(Target.class);
        assertThat(target.value()).containsExactlyInAnyOrder(ElementType.TYPE, ElementType.METHOD);
        assertThat(target.value()).doesNotContain(ElementType.FIELD, ElementType.PARAMETER);
    }

    @Test
    @DisplayName("边界：注解未被 @Inherited 标记，父类上的挂载不会被子类继承")
    void notInheritedBySubclass() {
        assertThat(InheritedFromAnnotated.class.getAnnotation(DataScope.class)).isNull();
    }
}
