package com.snowdrift.framework.log.config;

import com.mzt.logapi.service.ILogRecordService;
import com.mzt.logapi.starter.annotation.EnableLogRecord;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.SnowdriftDefaultLogServiceImpl;
import com.snowdrift.framework.log.SnowdriftLogRecordServiceImpl;
import com.snowdrift.framework.log.aspect.ApiLogAspect;
import com.snowdrift.framework.log.aspect.LoginLogAspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftLogConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 日志自动配置单元测试
 * @since 1.0.0
 */
class SnowdriftLogConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftLogConfiguration.class));

    /** 模拟 bizlog 的启用方式（README 推荐用法） */
    @Configuration
    @EnableLogRecord(tenant = "")
    static class BizlogEnabled {
    }

    @Configuration
    static class UserLogRecordService {
        @Bean
        ILogRecordService userLogRecordService() {
            return mock(ILogRecordService.class);
        }
    }

    @Configuration
    static class PrimaryUserLogRecordService {
        @Bean
        @Primary
        ILogRecordService userLogRecordService() {
            return mock(ILogRecordService.class);
        }
    }

    @Configuration
    static class UserAspect {
        static ApiLogAspect instance;

        @Bean
        ApiLogAspect userApiLogAspect(ILogService logService) {
            instance = new ApiLogAspect(logService);
            return instance;
        }
    }

    @Test
    @DisplayName("Servlet Web 环境下装配默认日志服务、桥接服务与两个切面")
    void registersDefaults() {
        webRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(context).hasSingleBean(ILogService.class);
            assertThat(context.getBean(ILogService.class)).isInstanceOf(SnowdriftDefaultLogServiceImpl.class);

            assertThat(context).hasSingleBean(ILogRecordService.class);
            assertThat(context.getBean(ILogRecordService.class)).isInstanceOf(SnowdriftLogRecordServiceImpl.class);

            assertThat(context).hasSingleBean(ApiLogAspect.class);
            assertThat(context).hasSingleBean(LoginLogAspect.class);
        });
    }

    @Test
    @DisplayName("启用 @EnableLogRecord（README 用法）后，生效的仍是框架桥接实现（回归：曾被 bizlog 默认实现顶掉）")
    void bridgeWinsOverBizlogDefault() {
        webRunner.withUserConfiguration(BizlogEnabled.class).run(context -> {
            assertThat(context).hasNotFailed();

            // bizlog 自己的默认实现也在容器里，但按类型注入必须拿到框架桥接
            assertThat(context).hasBean("recordService");
            assertThat(context.getBean("recordService"))
                    .isInstanceOf(com.mzt.logapi.service.impl.DefaultLogRecordServiceImpl.class);

            assertThat(context.getBean(ILogRecordService.class))
                    .isInstanceOf(SnowdriftLogRecordServiceImpl.class);
        });
    }

    @Test
    @DisplayName("业务自定义 ILogService 生效（@ConditionalOnMissingBean）")
    void customLogServiceWins() {
        ILogService custom = mock(ILogService.class);

        webRunner.withBean(ILogService.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(ILogService.class);
            assertThat(context.getBean(ILogService.class)).isSameAs(custom);
            // 桥接服务仍然注册，并注入业务自定义的日志服务
            assertThat(context).hasSingleBean(ILogRecordService.class);
        });
    }

    @Test
    @DisplayName("业务自定义操作日志服务：默认桥接优先；按文档关闭桥接后自定义实现接管")
    void customLogRecordServiceSemantics() {
        // A) 自定义实现未标注 @Primary：框架桥接（@Primary）按类型生效，两者并存
        webRunner.withUserConfiguration(UserLogRecordService.class).run(context -> {
            assertThat(context).hasBean("userLogRecordService");
            assertThat(context.getBean(ILogRecordService.class)).isInstanceOf(SnowdriftLogRecordServiceImpl.class);
        });

        // B) 关闭框架桥接（snowdrift.log.record-service=false）+ 自定义 @Primary 实现：业务实现接管
        webRunner.withUserConfiguration(PrimaryUserLogRecordService.class, BizlogEnabled.class)
                .withPropertyValues("snowdrift.log.record-service=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SnowdriftLogRecordServiceImpl.class);
                    assertThat(context.getBean(ILogRecordService.class))
                            .isNotInstanceOf(SnowdriftLogRecordServiceImpl.class);
                });
    }

    @Test
    @DisplayName("自定义实现也标 @Primary 却未关闭桥接时，按类型解析会因双 @Primary 失败（故必须按文档关闭桥接）")
    void dualPrimaryIsRejected() {
        webRunner.withUserConfiguration(PrimaryUserLogRecordService.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getBean(ILogRecordService.class))
                    .isInstanceOf(NoUniqueBeanDefinitionException.class);
        });
    }

    @Test
    @DisplayName("业务自定义切面时框架切面退让（避免双切面重复记录）")
    void customAspectWins() {
        webRunner.withUserConfiguration(UserAspect.class).run(context -> {
            assertThat(context).hasSingleBean(ApiLogAspect.class);
            assertThat(context.getBean(ApiLogAspect.class)).isSameAs(UserAspect.instance);
        });
    }

    @Test
    @DisplayName("切面注入的是容器中最终生效的 ILogService 实例")
    void aspectsUseTheEffectiveLogService() {
        ILogService custom = mock(ILogService.class);

        webRunner.withBean(ILogService.class, () -> custom).run(context -> {
            ApiLogAspect apiLogAspect = context.getBean(ApiLogAspect.class);
            LoginLogAspect loginLogAspect = context.getBean(LoginLogAspect.class);

            assertThat(ReflectionTestUtils.getField(apiLogAspect, "logService")).isSameAs(custom);
            assertThat(ReflectionTestUtils.getField(loginLogAspect, "logService")).isSameAs(custom);
        });
    }

    @Test
    @DisplayName("非 Web 环境（如纯定时任务应用）不装配任何 Bean")
    void backsOffOutsideWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftLogConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ILogService.class);
                    assertThat(context).doesNotHaveBean(ILogRecordService.class);
                    assertThat(context).doesNotHaveBean(ApiLogAspect.class);
                    assertThat(context).doesNotHaveBean(LoginLogAspect.class);
                });
    }
}
