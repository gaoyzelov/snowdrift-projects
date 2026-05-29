package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.auth.AnonymousAccessScanner;
import com.snowdrift.framework.security.spring.filter.SecurityContextFilter;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.store.TokenStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.ClassUtils;
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
@ConditionalOnProperty(prefix = "snowdrift.security.spring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowdriftSecuritySpringConfiguration {

    private final SpringSecurityProperties securityProperties;

    public SnowdriftSecuritySpringConfiguration(SpringSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void checkConflict() {
        if (ClassUtils.isPresent("com.snowdrift.framework.security.satoken.properties.SaTokenSecurityProperties", null)) {
            log.warn("[Snowdrift Security] 检测到 snowdrift-security-satoken 和 snowdrift-security-spring 同时存在，" +
                    "请通过 snowdrift.security.sa-token.enabled 或 snowdrift.security.spring.enabled 禁用其中一个模块");
        }
    }

    /**
     * 配置 Spring Security 过滤器链
     * <p>
     * 禁用 CSRF（REST API 场景），会话管理设为无状态，
     * 排除路径放行，其余请求由 {@link SecurityContextFilter} 桥接认证。
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
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        if (securityProperties.isCorsEnabled()) {
            http.cors(Customizer.withDefaults());
        }
        return http.build();
    }

    /**
     * 内存 TokenStore
     */
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore inMemoryTokenStore() {
        return new InMemoryTokenStore(securityProperties.getTimeout());
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
