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
 * Quartz 璋冨害鑷姩閰嶇疆
 * <p>
 * 褰?{@link Scheduler} 鍦ㄧ被璺緞涓彲鐢ㄤ笖 {@code snowdrift.schedule.quartz.enabled=true} 鏃舵縺娲汇€? * 鍦?XXL-JOB 鑷姩閰嶇疆涔嬪悗澶勭悊锛岃嫢 XXL-JOB 宸叉敞鍐?{@link IScheduleService} 鍒欒烦杩囥€? * </p>
 *
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

