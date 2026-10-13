package com.snowdrift.framework.orm.mp.config;

import com.snowdrift.framework.base.exception.BizException;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOrmMpInterceptorOverrideWarningTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 使用者自建 MybatisPlusInterceptor 时默认启动失败；显式允许后告警，扩展器可安全追加插件
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
    @DisplayName("默认禁止使用者整体替换 MybatisPlusInterceptor，启动失败并给出扩展建议")
    void failsWhenFrameworkInterceptorIsReplacedByDefault() {
        runner.withUserConfiguration(UserInterceptorConfiguration.class).run(context -> {
            assertThat(context).hasFailed();
            Throwable cause = rootCause(context.getStartupFailure());
            assertThat(cause)
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining(MARKER)
                    .hasMessageContaining("MybatisPlusInterceptorCustomizer");
        });
    }

    @Test
    @DisplayName("显式允许整体替换后启动并告警说明框架保护未注册")
    void allowsOverrideOnlyWhenExplicitlyConfigured(CapturedOutput output) {
        runner.withPropertyValues("snowdrift.orm.mp.allow-interceptor-override=true")
                .withUserConfiguration(UserInterceptorConfiguration.class).run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class))
                            .isSameAs(UserInterceptorConfiguration.USER_INTERCEPTOR);

                    assertThat(output).contains(MARKER);
                    assertThat(output).contains("多租户");
                    assertThat(output).contains("数据权限");
                });
    }

    @Test
    @DisplayName("无人覆盖时不告警，默认框架拦截器正常注册")
    void silentWhenFrameworkInterceptorApplied(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasBean("mybatisPlusInterceptor");
            assertThat(output).doesNotContain(MARKER);
        });
    }

    @Test
    @DisplayName("通过扩展器追加自定义拦截器，不替换框架默认保护")
    void customizerAppendsToFrameworkInterceptor() {
        runner.withUserConfiguration(UserCustomizerConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            assertThat(interceptor.getInterceptors())
                    .contains(UserCustomizerConfiguration.CUSTOM_INTERCEPTOR);
            assertThat(interceptor.getInterceptors())
                    .anyMatch(i -> i instanceof com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor);
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

    @Configuration(proxyBeanMethods = false)
    static class UserCustomizerConfiguration {

        static final InnerInterceptor CUSTOM_INTERCEPTOR = mock(InnerInterceptor.class);

        @Bean
        MybatisPlusInterceptorCustomizer mybatisPlusInterceptorCustomizer() {
            return interceptor -> interceptor.addInnerInterceptor(CUSTOM_INTERCEPTOR);
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
