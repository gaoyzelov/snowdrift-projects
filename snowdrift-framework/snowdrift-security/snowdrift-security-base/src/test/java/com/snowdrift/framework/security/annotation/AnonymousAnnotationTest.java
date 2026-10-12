package com.snowdrift.framework.security.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnonymousAnnotationTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 匿名访问注解黑盒测试：仅可标注方法、RUNTIME 保留、无成员（纯标记），
 * 且可通过反射从方法上取回
 * @since 1.0.0
 */
class AnonymousAnnotationTest {

    static class Controller {

        @Anonymous
        public void login() {
        }

        public void secured() {
        }
    }

    @Test
    @DisplayName("元注解契约：仅可标注 METHOD，且保留策略为 RUNTIME")
    void targetMethodOnlyAndRuntimeRetention() {
        Target target = Anonymous.class.getAnnotation(Target.class);
        assertThat(target.value()).containsExactly(ElementType.METHOD);

        Retention retention = Anonymous.class.getAnnotation(Retention.class);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("纯标记注解：不声明任何成员（无方法、无字段）")
    void markerHasNoMembers() {
        assertThat(Anonymous.class.getDeclaredMethods()).isEmpty();
        assertThat(Anonymous.class.getDeclaredFields()).isEmpty();
    }

    @Test
    @DisplayName("反射可见性：标注的方法能取回注解，未标注的方法取回 null")
    void retrievableOnAnnotatedMethod() throws Exception {
        Method login = Controller.class.getMethod("login");
        Method secured = Controller.class.getMethod("secured");

        assertThat(login.getAnnotation(Anonymous.class)).isNotNull();
        assertThat(secured.getAnnotation(Anonymous.class)).isNull();
    }
}
