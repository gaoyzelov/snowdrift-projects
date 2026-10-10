package com.snowdrift.framework.schedule.quartz.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuartzPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Quartz 配置默认值黑盒测试：enabled 默认关闭
 * @since 1.0.0
 */
class QuartzPropertiesTest {

    @Test
    @DisplayName("默认 enabled=false")
    void defaultDisabled() {
        assertThat(new QuartzProperties().getEnabled()).isFalse();
    }
}
