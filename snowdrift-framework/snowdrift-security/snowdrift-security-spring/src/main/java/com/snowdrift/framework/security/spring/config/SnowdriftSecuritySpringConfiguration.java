package com.snowdrift.framework.security.spring.config;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.base.util.ServletUtil;
import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.filter.SecurityContextFilter;
import com.snowdrift.framework.security.spring.handler.SpringSecurityExceptionHandler;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.service.SpringSecurityServiceImpl;
import com.snowdrift.framework.security.spring.store.AbstractTokenStore;
import com.snowdrift.framework.security.spring.store.InMemoryTokenStore;
import com.snowdrift.framework.security.spring.store.RedisTokenStore;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties.TokenStoreMode;
import com.snowdrift.framework.security.spring.util.AnonymousScanner;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
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
 * 与 Sa-Token 后端是否同时启用，通过 {@link #checkBackendExclusivity(boolean, boolean)} 在启动期直接失败。
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
        assertBackendsExclusive(environment);
    }

    /**
     * 配置 Spring Security 过滤器链
     * <p>
     * 禁用 CSRF（REST API 场景），会话管理设为无状态，
     * 排除路径放行，其余请求由 {@link SecurityContextFilter} 桥接认证。
     * 认证/鉴权异常返回 JSON 格式的 {@link Result}：HTTP 状态码固定 200，
     * 错误类型见 {@code body.code}（1001 未登录 / 1002 无权限）。
     * 禁用匿名认证：未登录请求不再被包装为 AnonymousAuthenticationToken，避免匿名被误判为已认证。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenStore tokenStore,
                                                   RequestMappingHandlerMapping handlerMapping) throws Exception {
        List<AnonymousScanner.Endpoint> anonymousEndpoints = AnonymousScanner.scan(handlerMapping);
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
                    // 按「路径 + 方法」放行：只按路径会连带放开同一路径上未被 @Anonymous 标注的方法
                    for (AnonymousScanner.Endpoint endpoint : anonymousEndpoints) {
                        if (endpoint.allMethods()) {
                            auth.requestMatchers(endpoint.pattern()).permitAll();
                        } else {
                            endpoint.methods().forEach(method ->
                                    auth.requestMatchers(method, endpoint.pattern()).permitAll());
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                // 直接在链内构造桥接过滤器，避免其同时作为 Spring Bean 被 Boot 注册为容器过滤器导致重复执行
                .addFilterBefore(new SecurityContextFilter(properties, tokenStore), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        // 统一响应契约：HTTP 状态码固定 200，成败与错误类型由 body.code 表达
                        //（与 WebExceptionHandler 的异常处理一致），故此处不再设 401/403。
                        // body.code 用业务码（1001/1002），文案用 ResultCode 内置中文，避免依赖 i18n 开关导致 key 透出。
                        .authenticationEntryPoint((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_OK,
                                        Result.err(ResultCode.UNAUTHORIZED.code(), ResultCode.UNAUTHORIZED.msg())))
                        .accessDeniedHandler((request, response, e) ->
                                ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_OK,
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
     * 默认 Redis TokenStore。使用者可提供自定义 {@link TokenStore} 覆盖；
     * 内存模式必须通过配置显式启用。
     */
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore tokenStore(ObjectProvider<RedisConnectionFactory> connectionFactories) {
        if (properties.getTokenStoreMode() == TokenStoreMode.MEMORY) {
            log.warn("[Snowdrift-Security] TokenStore 已显式配置为进程内 MEMORY 模式；"
                    + "该模式不在实例间共享，重启或扩缩容后登录态不会保留。仅建议用于开发或单实例部署。");
            return new InMemoryTokenStore(properties.getTimeout(), properties.getActiveTimeout());
        }

        RedisConnectionFactory connectionFactory = connectionFactories.getIfAvailable();
        if (connectionFactory == null) {
            throw new IllegalStateException("[Snowdrift-Security] TokenStore 默认模式为 REDIS，"
                    + "但容器中没有 RedisConnectionFactory。请配置 Redis 连接，"
                    + "或显式设置 snowdrift.security.spring.token-store-mode=memory "
                    + "（仅适用于开发/单实例部署），也可提供自定义 TokenStore Bean。");
        }
        return redisTokenStore(connectionFactory);
    }

    private TokenStore redisTokenStore(RedisConnectionFactory connectionFactory) {
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
     * 双后端互斥守卫
     * <p>
     * spring 与 sa-token 两个后端均为仅由各自 {@code enabled} 属性驱动的自动配置。
     * 两者同时启用时，最终生效的 {@link ISecurityService} 实现由自动配置顺序决定（不保证），
     * 表现为「谁在管认证」不可预测，且难以排查 —— 与 {@code MqActivationGuard} 处理多实现互斥的方式一致，
     * 这里在<b>启动期</b>直接失败，而不是只打一条容易被忽略的告警。
     * </p>
     * <p>
     * 仅当另一后端<b>模块在类路径上</b>且属性为 {@code true} 时才判定冲突：属性为 true 但实现不存在时
     * 该后端根本不会生效，不构成冲突。
     * </p>
     *
     * @param saTokenModulePresent sa-token 实现模块是否在类路径上
     * @param saTokenEnabled       {@code snowdrift.security.sa-token.enabled} 是否为 true
     */
    static void checkBackendExclusivity(boolean saTokenModulePresent, boolean saTokenEnabled) {
        if (saTokenModulePresent && saTokenEnabled) {
            throw new IllegalStateException("[Snowdrift-Security] spring 与 sa-token 两个安全后端均已启用"
                    + "（snowdrift.security.spring.enabled=true 且 snowdrift.security.sa-token.enabled=true）。"
                    + "同一时间只允许启用一个：生效实现由自动配置顺序决定，行为不可控。"
                    + "请将其中一个置为 false，或移除对应实现依赖。");
        }
    }

    private void assertBackendsExclusive(Environment environment) {
        checkBackendExclusivity(
                ClassUtils.isPresent(
                        "com.snowdrift.framework.security.satoken.config.SnowdriftSecuritySaTokenConfiguration",
                        getClass().getClassLoader()),
                environment.getProperty("snowdrift.security.sa-token.enabled",
                        Boolean.class, Boolean.FALSE));
    }
}
