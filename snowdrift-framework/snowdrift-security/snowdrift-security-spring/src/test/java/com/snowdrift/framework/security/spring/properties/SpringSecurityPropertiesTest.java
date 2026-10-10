package com.snowdrift.framework.security.spring.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringSecurityPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Spring Security 扩展配置默认值黑盒测试：csrf/cors 默认关闭，且继承公共属性默认值
 * @since 1.0.0
 */
class SpringSecurityPropertiesTest {

    @Test
    @DisplayName("默认值：csrf/cors 关闭，继承 header/timeout/prefix 公共默认")
    void defaults() {
        SpringSecurityProperties properties = new SpringSecurityProperties();

        assertThat(properties.getCsrfEnabled()).isFalse();
        assertThat(properties.getCorsEnabled()).isFalse();
        assertThat(properties.getEnabled()).isFalse();
        assertThat(properties.getHeaderName()).isEqualTo("Authorization");
        assertThat(properties.getTimeout()).isEqualTo(86400L);
        assertThat(properties.getPrefix()).isEqualTo("Bearer");
    }
}
