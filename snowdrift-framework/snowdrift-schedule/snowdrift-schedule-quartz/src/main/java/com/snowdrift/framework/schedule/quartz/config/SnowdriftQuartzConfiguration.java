package com.snowdrift.framework.schedule.quartz.config;

import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.quartz.service.QuartzScheduleServiceImpl;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Quartz 调度自动配置
 * <p>
 * 当 {@link Scheduler} 在类路径中可用且 {@code snowdrift.schedule.quartz.enabled=true} 时激活。
 * 在 XXL-JOB 自动配置之后处理，若 XXL-JOB 已注册 {@link IScheduleService} 则跳过。
 * </p>
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@EnableConfigurationProperties(QuartzProperties.class)
@AutoConfiguration(afterName = "com.snowdrift.framework.schedule.xxljob.config.SnowdriftXxlJobConfiguration")
@ConditionalOnProperty(prefix = "snowdrift.schedule.quartz", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowdriftQuartzConfiguration {

    @Bean
    @ConditionalOnMissingBean(IScheduleService.class)
    public IScheduleService quartzScheduleService(Scheduler scheduler, QuartzProperties properties) {
        return new QuartzScheduleServiceImpl(scheduler);
    }
}

