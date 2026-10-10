package com.snowdrift.framework.security.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 公共安全配置默认值黑盒测试：锁定 header/超时/前缀/放行路径等默认契约
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
}
