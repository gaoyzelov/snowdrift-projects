package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOrmMpInterceptorOverrideWarningTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 使用者自建 MybatisPlusInterceptor 时的诊断：多租户/数据权限/防全表拦截器会整体退避，
 *              必须留下 WARN 而不是静默失效
 * @since 1.0.0
 */
@ExtendWith(OutputCaptureExtension.class)
class SnowdriftOrmMpInterceptorOverrideWarningTest {

    /** 告警文案里用于识别的稳定片段 */
    private static final String MARKER = "MybatisPlusInterceptor 已被使用者提供的实例取代";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftOrmMpConfiguration.class))
            .withPropertyValues("snowdrift.orm.mp.crypto=false",
                    "snowdrift.orm.mp.tenant.enabled=true");

    @Test
    @DisplayName("使用者自建 MybatisPlusInterceptor：框架整体退避，并给出 WARN 说明哪些保护未注册")
    void warnsWhenFrameworkInterceptorIsReplaced(CapturedOutput output) {
        runner.withUserConfiguration(UserInterceptorConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context.getBean(MybatisPlusInterceptor.class))
                    .isSameAs(UserInterceptorConfiguration.USER_INTERCEPTOR);

            assertThat(output).contains(MARKER);
            assertThat(output).contains("多租户");
            assertThat(output).contains("数据权限");
        });
    }

    @Test
    @DisplayName("无人覆盖时不告警，避免假警报")
    void silentWhenFrameworkInterceptorApplied(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasBean("mybatisPlusInterceptor");
            assertThat(output).doesNotContain(MARKER);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserInterceptorConfiguration {

        static final MybatisPlusInterceptor USER_INTERCEPTOR = new MybatisPlusInterceptor();

        @Bean
        MybatisPlusInterceptor userMybatisPlusInterceptor() {
            return USER_INTERCEPTOR;
        }
    }
}
