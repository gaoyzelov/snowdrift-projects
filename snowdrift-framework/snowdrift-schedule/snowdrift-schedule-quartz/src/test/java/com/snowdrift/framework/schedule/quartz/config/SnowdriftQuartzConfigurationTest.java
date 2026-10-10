package com.snowdrift.framework.schedule.quartz.config;

import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.quartz.service.QuartzScheduleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftQuartzConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Quartz 自动配置黑盒测试：Bean 工厂方法、enabled 开关条件装配、@ConditionalOnMissingBean 覆盖
 * @since 1.0.0
 */
class SnowdriftQuartzConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftQuartzConfiguration.class))
            .withBean(Scheduler.class, () -> mock(Scheduler.class));

    @Test
    @DisplayName("工厂方法返回 QuartzScheduleServiceImpl")
    void beanMethodReturnsImpl() {
        IScheduleService<?, ?> service = new SnowdriftQuartzConfiguration()
                .quartzScheduleService(mock(Scheduler.class), new QuartzProperties());

        assertThat(service).isInstanceOf(QuartzScheduleServiceImpl.class);
    }

    @Test
    @DisplayName("enabled=true 时注册 IScheduleService")
    void registersWhenEnabled() {
        runner.withPropertyValues("snowdrift.schedule.quartz.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(IScheduleService.class);
                    assertThat(ctx.getBean(IScheduleService.class)).isInstanceOf(QuartzScheduleServiceImpl.class);
                });
    }

    @Test
    @DisplayName("未配置 enabled（默认 false）时不注册")
    void notRegisteredWhenDisabled() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(IScheduleService.class));
    }

    @Test
    @DisplayName("enabled=false 时不注册")
    void notRegisteredWhenExplicitlyDisabled() {
        runner.withPropertyValues("snowdrift.schedule.quartz.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(IScheduleService.class));
    }

    @Test
    @DisplayName("已存在用户自定义 IScheduleService 时让位（@ConditionalOnMissingBean）")
    void backsOffWhenUserBeanPresent() {
        IScheduleService<?, ?> userBean = mock(IScheduleService.class);
        runner.withPropertyValues("snowdrift.schedule.quartz.enabled=true")
                .withBean(IScheduleService.class, () -> userBean)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(IScheduleService.class);
                    assertThat(ctx.getBean(IScheduleService.class)).isSameAs(userBean);
                });
    }
}
