package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.spring.store.RedisTokenStore;
import com.snowdrift.framework.security.spring.store.TokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftSecuritySpringConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Spring Security 自动配置的 Bean 工厂黑盒测试：TokenStore 内存/Redis 实现选择、服务与异常处理器装配
 *               （securityFilterChain 需完整 HttpSecurity 上下文，属集成级，未在此覆盖）
 * @since 1.0.0
 */
class SnowdriftSecuritySpringConfigurationTest {

    private final SpringSecurityProperties properties = new SpringSecurityProperties();
    private final SnowdriftSecuritySpringConfiguration configuration =
            new SnowdriftSecuritySpringConfiguration(properties, new MockEnvironment());

    @Test
    @DisplayName("无 Redis 时使用内存 TokenStore")
    void inMemoryTokenStoreBean() {
        TokenStore tokenStore = configuration.inMemoryTokenStore();

        assertThat(tokenStore).isInstanceOf(InMemoryTokenStore.class);
    }

    @Test
    @DisplayName("存在 RedisConnectionFactory 时使用 Redis TokenStore")
    void redisTokenStoreBean() {
        TokenStore tokenStore = configuration.redisTokenStore(mock(RedisConnectionFactory.class));

        assertThat(tokenStore).isInstanceOf(RedisTokenStore.class);
    }

    @Test
    @DisplayName("注册 ISecurityService 的 Spring Security 实现")
    void securityServiceBean() {
        ISecurityService service = configuration.securityService(configuration.inMemoryTokenStore());

        assertThat(service).isInstanceOf(SpringSecurityServiceImpl.class);
    }

    @Test
    @DisplayName("注册 Spring Security 异常处理器")
    void exceptionHandlerBean() {
        assertThat(configuration.springSecurityExceptionHandler())
                .isInstanceOf(SpringSecurityExceptionHandler.class);
    }
}
