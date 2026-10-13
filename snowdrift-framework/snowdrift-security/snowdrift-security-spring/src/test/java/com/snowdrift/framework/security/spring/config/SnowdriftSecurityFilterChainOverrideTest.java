package com.snowdrift.framework.security.spring.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftSecurityFilterChainOverrideTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description SecurityFilterChain 的装配优先级：框架链必须能被使用者自定义的链覆盖（不出现双链），
 *              且在无人提供时仍能注册，并让 Spring Boot 自带的默认链退避
 * @since 1.0.0
 */
class SnowdriftSecurityFilterChainOverrideTest {

    /**
     * 连同 Spring Boot 自带的默认链一起装配，用于验证「框架链先注册、Boot 默认链退避」的顺序依赖。
     * Boot 的默认链由 {@code DefaultWebSecurityCondition}（REGISTER_BEAN 阶段 +
     * {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}）把守，胜负纯由注册顺序决定；
     * 该类（{@code SpringBootWebSecurityConfiguration}）是包级私有，无法直接引用，
     * 但它由 {@code SecurityAutoConfiguration} 的 {@code @Import} 带入，故此处只需装配后者。
     */
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    SnowdriftSecuritySpringConfiguration.class))
            .withUserConfiguration(StubHandlerMappingConfiguration.class)
            .withPropertyValues("snowdrift.security.spring.enabled=true",
                    "snowdrift.security.spring.token-store-mode=memory");

    @Test
    @DisplayName("无人提供 SecurityFilterChain 时注册框架链，且 Boot 默认链退避（全局仅一条）")
    void frameworkChainRegisteredAndBootDefaultBacksOff() {
        runner.run(context -> {
            assertThat(context).hasBean("securityFilterChain");
            assertThat(context.getBeansOfType(SecurityFilterChain.class))
                    .containsOnlyKeys("securityFilterChain");
        });
    }

    @Test
    @DisplayName("使用者自定义 SecurityFilterChain 时框架链退避：不出现双链，且生效的是使用者的链")
    void userDefinedChainWinsWithoutDuplication() {
        runner.withUserConfiguration(UserChainConfiguration.class).run(context -> {
            assertThat(context).doesNotHaveBean("securityFilterChain");
            assertThat(context.getBeansOfType(SecurityFilterChain.class))
                    .containsOnlyKeys("userSecurityFilterChain");
            assertThat(context.getBean("userSecurityFilterChain"))
                    .isSameAs(UserChainConfiguration.CHAIN);
        });
    }

    /**
     * securityFilterChain 需要 RequestMappingHandlerMapping（用于扫描 @Anonymous 端点）。
     * 这里给一个未初始化的裸实例 —— 它没有任何注册项，AnonymousScanner 扫出空路径列表。
     */
    @Configuration(proxyBeanMethods = false)
    static class StubHandlerMappingConfiguration {

        @Bean
        RequestMappingHandlerMapping requestMappingHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }

        /**
         * authorizeHttpRequests 传字符串模式时，Spring Security 用 MvcRequestMatcher，
         * 而后者要求容器里有名为 mvcHandlerMappingIntrospector 的 Bean。
         * 真实应用由 WebMvcAutoConfiguration 提供，这里的最小上下文需自行补上。
         */
        @Bean
        HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
            return new HandlerMappingIntrospector();
        }
    }

    /**
     * 模拟使用者自定义过滤器链。使用者的链不必由 HttpSecurity 构建，
     * 因此这里用最朴素的 DefaultSecurityFilterChain，专注验证「谁胜出」。
     */
    @Configuration(proxyBeanMethods = false)
    static class UserChainConfiguration {

        static final SecurityFilterChain CHAIN = new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE);

        @Bean
        SecurityFilterChain userSecurityFilterChain() {
            return CHAIN;
        }
    }
}
