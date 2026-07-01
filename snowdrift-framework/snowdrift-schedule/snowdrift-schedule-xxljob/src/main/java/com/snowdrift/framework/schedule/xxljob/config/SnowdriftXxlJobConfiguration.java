package com.snowdrift.framework.schedule.xxljob.config;

import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.xxljob.service.XxlJobScheduleServiceImpl;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * XXL-JOB 调度自动配置
 * <p>
 * 当 {@link XxlJobSpringExecutor} 在类路径中且 {@code snowdrift.schedule.xxl-job.enabled=true} 时激活。
 * 优先于 Quartz 注册 {@link IScheduleService} 实现。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnClass(XxlJobSpringExecutor.class)
@ConditionalOnProperty(prefix = "snowdrift.schedule.xxl-job", name = "enabled", havingValue = "true")
public class SnowdriftXxlJobConfiguration {

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAppname(properties.getAppName());
        executor.setAccessToken(properties.getAccessToken());
        executor.setIp(properties.getIp());
        executor.setPort(properties.getPort());
        executor.setAddress(properties.getAddress());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());
        log.info("XXL-JOB 执行器初始化完成: appName={}, port={}", properties.getAppName(), properties.getPort());
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(IScheduleService.class)
    public IScheduleService xxlJobScheduleService(XxlJobProperties properties) {
        return new XxlJobScheduleServiceImpl(properties);
    }
}
