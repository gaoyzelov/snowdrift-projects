package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.base.util.ServletUtil;
import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.util.AnonymousScanner;
import com.snowdrift.framework.security.spring.filter.SecurityContextFilter;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.AbstractTokenStore;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.spring.store.RedisTokenStore;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.web.util.I18nUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
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
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * Spring Security 安全模块自动配置
 * <p>
 * 以 {@code snowdrift.security.spring.*} 为配置入口，
 * 通过 {@code snowdrift.security.spring.enabled} 控制模块开关。
 * 仅由自身 enabled 属性驱动，不受类路径上是否存在 Sa-Token（{@code SaTokenConfig}）影响；
 * 与 Sa-Token 后端是否同时启用，通过 {@link #warnIfBothBackendsEnabled(Environment)} 给出诊断。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SpringSecurityProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.security.spring", name = "enabled", havingValue = "true")
public class SnowdriftSecuritySpringConfiguration {

    private final SpringSecurityProperties properties;

    public SnowdriftSecuritySpringConfiguration(SpringSecurityProperties properties, Environment environment) {
        this.properties = properties;
        warnIfBothBackendsEnabled(environment);
    }

    /**
     * 配置 Spring Security 过滤器链
     * <p>
     * 禁用 CSRF（REST API 场景），会话管理设为无状态，
     * 排除路径放行，其余请求由 {@link SecurityContextFilter} 桥接认证。
     * 认证/鉴权异常返回 JSON 格式的 {@link Result} 响应（401 / 403）。
     * 禁用匿名认证：未登录请求不再被包装为 AnonymousAuthenticationToken，避免匿名被误判为已认证。
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenStore tokenStore,
                                                   RequestMappingHandlerMapping handlerMapping) throws Exception {
        List<String> anonymousPaths = AnonymousScanner.scan(handlerMapping);
        http
                .csrf(csrf -> {
                    if (!properties.getCsrfEnabled()) {
                        csrf.disable();
                    }
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    for (String pattern : properties.getExcludePathPatterns()) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    for (String pattern : anonymousPaths) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                // 直接在链内构造桥接过滤器，避免其同时作为 Spring Bean 被 Boot 注册为容器过滤器导致重复执行
                .addFilterBefore(new SecurityContextFilter(properties, tokenStore), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        // body.code 用业务码（1001/1002），文案用 ResultCode 内置中文，避免依赖 i18n 开关导致 key 透出
                        .authenticationEntryPoint((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Result.err(ResultCode.UNAUTHORIZED.code(), ResultCode.UNAUTHORIZED.msg())))
                        .accessDeniedHandler((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                        Result.err(ResultCode.FORBIDDEN.code(), ResultCode.FORBIDDEN.msg())))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        if (properties.getCorsEnabled()) {
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
                properties.getTimeout(),
                properties.getActiveTimeout(),
                properties.getHeaderName() + StrConst.COLON + "token");
    }

    /**
     * 内存 TokenStore（Redis 不可用时的默认实现）
     */
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore inMemoryTokenStore() {
        return new InMemoryTokenStore(properties.getTimeout(), properties.getActiveTimeout());
    }

    /**
     * ISecurityService 的 Spring Security 实现
     */
    @Bean
    @ConditionalOnMissingBean(ISecurityService.class)
    public ISecurityService securityService(TokenStore tokenStore) {
        return new SpringSecurityServiceImpl(properties, tokenStore);
    }

    /**
     * Spring Security 异常全局处理器
     */
    @Bean
    @ConditionalOnMissingBean(SpringSecurityExceptionHandler.class)
    public SpringSecurityExceptionHandler springSecurityExceptionHandler() {
        return new SpringSecurityExceptionHandler();
    }

    /**
     * 双后端互斥诊断
     * <p>
     * spring 与 sa-token 两个后端均为仅由各自 {@code enabled} 属性驱动的自动配置。
     * 当两者同时启用时，最终由自动配置顺序决定生效的 {@link ISecurityService} 实现，行为不可控，
     * 此处给出明确告警提示用户仅启用其中一个。
     * </p>
     *
     * @param environment Spring 环境，用于读取另一后端的启用属性
     */
    private void warnIfBothBackendsEnabled(Environment environment) {
        boolean saTokenModulePresent = ClassUtils.isPresent(
                "com.snowdrift.framework.security.satoken.config.SnowdriftSecuritySaTokenConfiguration",
                getClass().getClassLoader());
        if (saTokenModulePresent
                && Boolean.TRUE.equals(environment.getProperty("snowdrift.security.sa-token.enabled",
                Boolean.class, Boolean.FALSE))) {
            log.warn("[Snowdrift-Security] 检测到 spring 与 sa-token 两个安全后端均已启用"
                    + "（snowdrift.security.spring.enabled=true 且 snowdrift.security.sa-token.enabled=true）。"
                    + "最终生效实现由自动配置顺序决定，行为不可控；请仅启用其中一个"
                    + "（将另一后端 enabled 置为 false，或移除对应实现依赖）。");
        }
    }
}
