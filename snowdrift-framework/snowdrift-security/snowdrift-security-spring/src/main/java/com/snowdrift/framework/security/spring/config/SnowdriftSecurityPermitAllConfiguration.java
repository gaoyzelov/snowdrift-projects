package com.snowdrift.framework.security.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 未启用时的放行（fail-open）自动配置
 * <p>
 * 仅当 {@code snowdrift.security.spring.enabled} 未配置或为 {@code false} 时生效：
 * 只要类路径上存在本模块（连带引入 {@code spring-boot-starter-security}），
 * Spring Boot 的默认安全链（自动生成密码 + 全量拦截）就会锁死应用。此处注册一条
 * 放行所有请求的最小 {@link SecurityFilterChain}，抢占并抑制 Boot 默认链，避免应用被意外锁定。
 * </p>
 * <p>
 * <b>注意：</b>此状态下应用是「放行不设防」的（fail-open），与其它模块关闭开关后的表现一致。
 * 若宿主应用自行声明了 {@link SecurityFilterChain}（或启用了真实的
 * {@link SnowdriftSecuritySpringConfiguration}），本配置会通过属性 + Bean 缺失条件自动退避，不会与其竞争。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/8
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(before = SecurityAutoConfiguration.class)
@ConditionalOnProperty(prefix = "snowdrift.security.spring", name = "enabled",
        havingValue = "false", matchIfMissing = true)
@ConditionalOnMissingBean(SecurityFilterChain.class)
public class SnowdriftSecurityPermitAllConfiguration {

    /**
     * 放行所有请求的最小过滤器链
     * <p>
     * CSRF / 表单登录 / HTTP Basic / 登出 / 匿名认证全部关闭，会话无状态，
     * 所有请求 {@code permitAll}，仅用于在 spring 后端关闭时抑制 Boot 默认安全链。
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[Snowdrift-Security] snowdrift.security.spring 未启用，注册 permit-all 安全链（fail-open），"
                + "Spring Boot 默认安全链已被抑制，应用不受 Spring Security 保护。");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
