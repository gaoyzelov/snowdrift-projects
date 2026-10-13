package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.spring.store.RedisTokenStore;
import com.snowdrift.framework.security.spring.store.TokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Spring Security 自动配置的 TokenStore 模式及核心 Bean 装配测试。
 */
class SnowdriftSecuritySpringConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftSecuritySpringConfiguration.class))
            .withPropertyValues("snowdrift.security.spring.enabled=true")
            // 避免在本测试中构造完整 Web 安全过滤器链；TokenStore 自动配置仍按真实上下文执行。
            .withBean("testSecurityFilterChain", SecurityFilterChain.class,
                    () -> mock(SecurityFilterChain.class));

    @Test
    @DisplayName("默认 REDIS 模式缺少 RedisConnectionFactory 时启动失败并给出可操作提示")
    void redisIsRequiredByDefault() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context.getStartupFailure()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("TokenStore 默认模式为 REDIS")
                    .hasMessageContaining("token-store-mode=memory")
                    .hasMessageContaining("自定义 TokenStore Bean");
        });
    }

    @Test
    @DisplayName("显式启用 MEMORY 模式时使用进程内 TokenStore")
    void memoryModeMustBeExplicit() {
        runner.withPropertyValues("snowdrift.security.spring.token-store-mode=memory")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TokenStore.class)).isInstanceOf(InMemoryTokenStore.class);
                    assertThat(context.getBean(ISecurityService.class)).isInstanceOf(SpringSecurityServiceImpl.class);
                    assertThat(context.getBean(SpringSecurityExceptionHandler.class))
                            .isInstanceOf(SpringSecurityExceptionHandler.class);
                });
    }

    @Test
    @DisplayName("REDIS 模式存在 RedisConnectionFactory 时使用 Redis TokenStore")
    void redisModeUsesRedisTokenStore() {
        runner.withBean("redisConnectionFactory", RedisConnectionFactory.class,
                        () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TokenStore.class)).isInstanceOf(RedisTokenStore.class);
                });
    }

    @Test
    @DisplayName("业务自定义 TokenStore 优先于默认 REDIS 要求")
    void customTokenStoreOverridesBackendSelection() {
        TokenStore customTokenStore = mock(TokenStore.class);
        runner.withBean("customTokenStore", TokenStore.class, () -> customTokenStore)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TokenStore.class)).isSameAs(customTokenStore);
                });
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
