package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.common.util.ServletUtil;
import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.auth.AnonymousAccessScanner;
import com.snowdrift.framework.security.spring.filter.SecurityContextFilter;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.AbstractTokenStore;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.spring.store.RedisTokenStore;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.web.i18n.I18nUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * Spring Security 安全模块自动配置
 * <p>
 * 以 {@code snowdrift.security.spring.*} 为配置入口，
 * 通过 {@code snowdrift.security.spring.enabled} 控制模块开关。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SpringSecurityProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.security.spring", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(type = "cn.dev33.satoken.config.SaTokenConfig")
public class SnowdriftSecuritySpringConfiguration {

    private final SpringSecurityProperties securityProperties;

    public SnowdriftSecuritySpringConfiguration(SpringSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 配置 Spring Security 过滤器链
     * <p>
     * 禁用 CSRF（REST API 场景），会话管理设为无状态，
     * 排除路径放行，其余请求由 {@link SecurityContextFilter} 桥接认证。
     * 认证/鉴权异常返回 JSON 格式的 {@link Result} 响应（401 / 403）。
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextFilter securityContextFilter,
                                                   ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) throws Exception {
        RequestMappingHandlerMapping hm = handlerMappingProvider.getIfAvailable();
        List<String> anonymousPaths = hm != null ? AnonymousAccessScanner.scan(hm) : List.of();
        http
                .csrf(csrf -> {
                    if (!securityProperties.isCsrfEnabled()) {
                        csrf.disable();
                    }
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    for (String pattern : securityProperties.getExcludePathPatterns()) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    for (String pattern : anonymousPaths) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(securityContextFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Result.err(HttpServletResponse.SC_UNAUTHORIZED,
                                                I18nUtil.getMessage("security.not.authenticated"))))
                        .accessDeniedHandler((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                        Result.err(HttpServletResponse.SC_FORBIDDEN,
                                                I18nUtil.getMessage("security.permission.denied"))))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        if (securityProperties.isCorsEnabled()) {
            http.cors(Customizer.withDefaults());
        }
        return http.build();
    }

    /**
     * Redis TokenStore（当容器中存在 RedisTemplate 时优先使用）
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore redisTokenStore(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(AbstractTokenStore.TokenEntry.class));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(AbstractTokenStore.TokenEntry.class));
        template.afterPropertiesSet();
        return new RedisTokenStore(template,
                securityProperties.getTimeout(),
                securityProperties.getActiveTimeout(),
                securityProperties.getHeaderName() + ":token:");
    }

    /**
     * 内存 TokenStore（Redis 不可用时的默认实现）
     */
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore inMemoryTokenStore() {
        return new InMemoryTokenStore(securityProperties.getTimeout(), securityProperties.getActiveTimeout());
    }

    /**
     * SecurityContext 桥接过滤器
     */
    @Bean
    public SecurityContextFilter securityContextFilter(TokenStore tokenStore) {
        return new SecurityContextFilter(securityProperties, tokenStore);
    }

    /**
     * ISecurityService 的 Spring Security 实现
     */
    @Bean
    @ConditionalOnMissingBean(ISecurityService.class)
    public ISecurityService securityService(TokenStore tokenStore) {
        return new SpringSecurityServiceImpl(securityProperties, tokenStore);
    }

    /**
     * Spring Security 异常全局处理器
     */
    @Bean
    @ConditionalOnMissingBean(SpringSecurityExceptionHandler.class)
    public SpringSecurityExceptionHandler springSecurityExceptionHandler() {
        return new SpringSecurityExceptionHandler();
    }
}
