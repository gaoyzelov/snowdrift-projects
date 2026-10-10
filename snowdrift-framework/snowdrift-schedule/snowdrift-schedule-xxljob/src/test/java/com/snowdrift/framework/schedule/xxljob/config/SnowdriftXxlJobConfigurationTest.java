package com.snowdrift.framework.schedule.xxljob.config;

import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.xxljob.service.XxlJobScheduleServiceImpl;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * SnowdriftXxlJobConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XXL-JOB 自动配置 Bean 工厂方法黑盒测试：调度服务实现类型、执行器属性透传（含 null 可选字段不抛异常）
 * @since 1.0.0
 */
class SnowdriftXxlJobConfigurationTest {

    private final SnowdriftXxlJobConfiguration configuration = new SnowdriftXxlJobConfiguration();

    private XxlJobProperties props() {
        XxlJobProperties p = new XxlJobProperties();
        p.setAdminAddresses("http://admin");
        p.setAppName("snowdrift-job");
        p.setUsername("admin");
        p.setPassword("123456");
        return p;
    }

    @Test
    @DisplayName("xxlJobScheduleService 返回 XxlJobScheduleServiceImpl")
    void scheduleServiceBean() {
        IScheduleService<?, ?> service = configuration.xxlJobScheduleService(props());

        assertThat(service).isInstanceOf(XxlJobScheduleServiceImpl.class);
    }

    @Test
    @DisplayName("xxlJobSpringExecutor 构建执行器；ip/address/accessToken 为 null 时不抛异常")
    void executorBeanToleratesNullOptionalFields() {
        XxlJobProperties p = props();
        p.setIp(null);
        p.setAddress(null);
        p.setAccessToken(null);

        assertThatCode(() -> {
            XxlJobSpringExecutor executor = configuration.xxlJobSpringExecutor(p);
            assertThat(executor).isNotNull();
        }).doesNotThrowAnyException();
    }
}
