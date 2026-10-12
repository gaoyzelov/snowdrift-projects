package com.snowdrift.framework.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 公共安全配置黑盒测试：锁定 header/超时/前缀/放行路径等默认契约，
 * 覆盖放行路径列表的不可变默认值、setter 覆盖以及 Bean Validation 注解契约
 * @since 1.0.0
 */
class SecurityPropertiesTest {

    @Test
    @DisplayName("默认值：enabled=false、header=Authorization、timeout=86400、activeTimeout=1800、prefix=Bearer")
    void defaults() {
        SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getEnabled()).isFalse();
        assertThat(properties.getHeaderName()).isEqualTo("Authorization");
        assertThat(properties.getTimeout()).isEqualTo(86400L);
        assertThat(properties.getActiveTimeout()).isEqualTo(1800L);
        assertThat(properties.getPrefix()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("默认放行路径包含静态资源/文档/错误页")
    void defaultExcludePaths() {
        SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getExcludePathPatterns())
                .contains("/favicon.ico", "/v3/api-docs/**", "/doc.html", "/error");
    }

    @Test
    @DisplayName("默认放行路径恰好 7 条且无重复，顺序与声明一致")
    void defaultExcludePathsExactAndDistinct() {
        SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getExcludePathPatterns()).containsExactly(
                "/favicon.ico",
                "/swagger-resources/**",
                "/v2/api-docs/**",
                "/v3/api-docs/**",
                "/doc.html",
                "/swagger-ui.html",
                "/error");
        assertThat(properties.getExcludePathPatterns()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("边界：默认放行路径由 List.of 构造，直接修改会抛 UnsupportedOperationException")
    void defaultExcludePathsAreImmutable() {
        SecurityProperties properties = new SecurityProperties();

        assertThatThrownBy(() -> properties.getExcludePathPatterns().add("/custom"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("setter：可整体替换默认值，替换后不再受默认列表约束")
    void setterOverridesAllFields() {
        SecurityProperties properties = new SecurityProperties();
        List<String> custom = new ArrayList<>(List.of("/public/**"));

        properties.setEnabled(Boolean.TRUE);
        properties.setHeaderName("X-Token");
        properties.setTimeout(60L);
        properties.setActiveTimeout(30L);
        properties.setPrefix("Token");
        properties.setExcludePathPatterns(custom);

        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getHeaderName()).isEqualTo("X-Token");
        assertThat(properties.getTimeout()).isEqualTo(60L);
        assertThat(properties.getActiveTimeout()).isEqualTo(30L);
        assertThat(properties.getPrefix()).isEqualTo("Token");
        assertThat(properties.getExcludePathPatterns()).containsExactly("/public/**");

        // 替换进去的是可变列表，允许后续追加（默认不可变列表不具备的行为）
        custom.add("/health");
        assertThat(properties.getExcludePathPatterns()).containsExactly("/public/**", "/health");
    }

    @Test
    @DisplayName("Bean Validation 契约：enabled/timeout/activeTimeout 非空、header 非空白、放行路径非空")
    void validationAnnotationsPresent() throws Exception {
        assertThat(field("enabled").getAnnotation(NotNull.class)).isNotNull();
        assertThat(field("headerName").getAnnotation(NotBlank.class)).isNotNull();
        assertThat(field("timeout").getAnnotation(NotNull.class)).isNotNull();
        assertThat(field("activeTimeout").getAnnotation(NotNull.class)).isNotNull();
        assertThat(field("excludePathPatterns").getAnnotation(NotEmpty.class)).isNotNull();

        // prefix 与数值下界没有约束注解，负值/空白由语义自行解释（<=0 表示不过期）
        assertThat(field("prefix").getAnnotations()).isEmpty();
    }

    @Test
    @DisplayName("边界：timeout/activeTimeout 允许 <=0（语义为不过期），setter 不做数值校验")
    void nonPositiveTimeoutsAccepted() {
        SecurityProperties properties = new SecurityProperties();

        properties.setTimeout(0L);
        properties.setActiveTimeout(-1L);
        properties.setPrefix(null);

        assertThat(properties.getTimeout()).isZero();
        assertThat(properties.getActiveTimeout()).isEqualTo(-1L);
        assertThat(properties.getPrefix()).isNull();
    }

    private static Field field(String name) throws NoSuchFieldException {
        return SecurityProperties.class.getDeclaredField(name);
    }
}
