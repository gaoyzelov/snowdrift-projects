package com.snowdrift.framework.security.satoken.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SaTokenSecurityPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Sa-Token 扩展配置默认值黑盒测试：并发/共享/日志默认关闭、最大登录数与 token 风格默认值
 * @since 1.0.0
 */
class SaTokenSecurityPropertiesTest {

    @Test
    @DisplayName("默认值：concurrent/isShare/isLog 关闭，maxLoginCount=12，tokenStyle=uuid，且继承公共默认")
    void defaults() {
        SaTokenSecurityProperties properties = new SaTokenSecurityProperties();

        assertThat(properties.getConcurrent()).isFalse();
        assertThat(properties.getIsShare()).isFalse();
        assertThat(properties.getIsLog()).isFalse();
        assertThat(properties.getMaxLoginCount()).isEqualTo(12);
        assertThat(properties.getTokenStyle()).isEqualTo("uuid");
        // 继承自 SecurityProperties 的公共默认
        assertThat(properties.getEnabled()).isFalse();
        assertThat(properties.getHeaderName()).isEqualTo("Authorization");
    }
}
