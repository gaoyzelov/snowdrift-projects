package com.snowdrift.framework.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftAsyncConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 异步配置黑盒测试：getAsyncExecutor 返回注入的线程池、未捕获异常处理器可用且不抛出
 * @since 1.0.0
 */
class SnowdriftAsyncConfigurationTest {

    @Test
    @DisplayName("getAsyncExecutor 返回构造注入的执行器（同一实例）")
    void returnsInjectedExecutor() {
        Executor executor = mock(Executor.class);
        SnowdriftAsyncConfiguration configuration = new SnowdriftAsyncConfiguration(executor);

        assertThat(configuration.snowdriftAsyncConfigurer().getAsyncExecutor()).isSameAs(executor);
    }

    @Test
    @DisplayName("未捕获异常处理器非空，处理异常时仅记录日志不抛出")
    void uncaughtExceptionHandlerLogsOnly() throws NoSuchMethodException {
        SnowdriftAsyncConfiguration configuration = new SnowdriftAsyncConfiguration(mock(Executor.class));
        AsyncUncaughtExceptionHandler exceptionHandler =
                configuration.snowdriftAsyncConfigurer().getAsyncUncaughtExceptionHandler();
        Method method = Object.class.getMethod("toString");

        assertThat(exceptionHandler).isNotNull();
        assertThatCode(() -> exceptionHandler.handleUncaughtException(new RuntimeException("async boom"), method, "p1"))
                .doesNotThrowAnyException();
    }

    private ApplicationContextRunner asyncRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SnowdriftExecutorConfiguration.class, SnowdriftAsyncConfiguration.class))
                .withPropertyValues("snowdrift.async.enabled=true");
    }

    @Test
    @DisplayName("宿主自定义 AsyncConfigurer 时框架退避：容器中只允许存在一个，否则首次 @Async 调用抛 IllegalStateException")
    void backsOffWhenHostProvidesAsyncConfigurer() {
        AsyncConfigurer hostConfigurer = mock(AsyncConfigurer.class);

        asyncRunner()
                .withBean("hostAsyncConfigurer", AsyncConfigurer.class, () -> hostConfigurer)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(AsyncConfigurer.class);
                    assertThat(ctx.getBean(AsyncConfigurer.class)).isSameAs(hostConfigurer);
                });
    }

    @Test
    @DisplayName("无宿主自定义时框架注册 AsyncConfigurer，并复用 snowdriftAsyncExecutor")
    void registersOwnConfigurerByDefault() {
        asyncRunner().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(AsyncConfigurer.class);
            assertThat(ctx.getBean(AsyncConfigurer.class).getAsyncExecutor())
                    .isSameAs(ctx.getBean("snowdriftAsyncExecutor"));
        });
    }
}
