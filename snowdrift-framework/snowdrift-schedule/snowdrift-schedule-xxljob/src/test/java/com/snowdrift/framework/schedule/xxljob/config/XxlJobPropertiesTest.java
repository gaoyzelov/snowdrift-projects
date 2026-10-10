package com.snowdrift.framework.schedule.xxljob.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XxlJobPropertiesTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XXL-JOB 配置默认值黑盒测试：锁定 enabled/appName/loginTokenKey/超时/端口/日志等默认契约
 * @since 1.0.0
 */
class XxlJobPropertiesTest {

    @Test
    @DisplayName("默认值：enabled=false、appName=snowdrift-job、tokenKey/超时/端口/日志保留天数符合约定")
    void defaults() {
        XxlJobProperties p = new XxlJobProperties();

        assertThat(p.getEnabled()).isFalse();
        assertThat(p.getAppName()).isEqualTo("snowdrift-job");
        assertThat(p.getLoginTokenKey()).isEqualTo("xxl_job_login_token");
        assertThat(p.getLoginTokenTimeout()).isEqualTo(Duration.ofHours(1));
        assertThat(p.getPort()).isEqualTo(9999);
        assertThat(p.getLogPath()).isEqualTo("logs/xxl-job");
        assertThat(p.getLogRetentionDays()).isEqualTo(30);
        assertThat(p.getAdminTimeout()).isEqualTo(5);
    }
}
