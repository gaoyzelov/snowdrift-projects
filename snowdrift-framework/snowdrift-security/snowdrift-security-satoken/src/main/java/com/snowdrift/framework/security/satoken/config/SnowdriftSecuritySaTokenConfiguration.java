package com.snowdrift.framework.security.satoken.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.listener.SaTokenListener;
import com.snowdrift.framework.security.satoken.interceptor.SecurityInterceptor;
import com.snowdrift.framework.security.satoken.handler.SaTokenExceptionHandler;
import com.snowdrift.framework.security.satoken.listener.TokenStateListener;
import com.snowdrift.framework.security.satoken.properties.SaTokenSecurityProperties;
import com.snowdrift.framework.security.satoken.service.SaTokenSecurityServiceImpl;
import com.snowdrift.framework.security.service.ISecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

/**
 * Sa-Token 安全模块自动配置
 * <p>
 * 以 {@code snowdrift.security.sa-token.*} 为配置入口，创建并接管 {@link SaTokenConfig}。
 * 通过 {@code snowdrift.security.sa-token.enabled} 控制整个安全模块的开关。
 * </p>
 *
 * @author 83674
 * @date 2026/5/15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SaTokenSecurityProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.security.sa-token", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowdriftSecuritySaTokenConfiguration implements WebMvcConfigurer {

    private final SaTokenSecurityProperties securityProperties;

    public SnowdriftSecuritySaTokenConfiguration(SaTokenSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void checkConflict() {
        if (ClassUtils.isPresent("com.snowdrift.framework.security.spring.properties.SpringSecurityProperties", null)) {
            log.warn("[Snowdrift Security] 检测到 snowdrift-security-satoken 和 snowdrift-security-spring 同时存在，" +
                    "请通过 snowdrift.security.sa-token.enabled 或 snowdrift.security.spring.enabled 禁用其中一个模块");
        }
    }

    /**
     * 以 {@code snowdrift.security.*} 的值为准，创建 Sa-Token 配置
     * <p>
     * 仅在 {@code snowdrift.security.enabled=true}（默认）时生效。
     * 关闭后 Sa-Token 使用其自身默认值，且拦截器不会注册。
     * </p>
     */
    @Bean
    @Primary
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        // Token 名称（对应请求头字段名）
        config.setTokenName(securityProperties.getHeaderName());
        // Token 有效期（秒）
        config.setTimeout(securityProperties.getTimeout());
        // Token 最低活跃频率（秒），超时冻结
        config.setActiveTimeout(securityProperties.getActiveTimeout());
        // 是否允许多端并发登录
        config.setIsConcurrent(securityProperties.isConcurrent());
        // Token 前缀（如 Bearer）
        config.setTokenPrefix(securityProperties.getPrefix());
        // 多人登录是否共用一个 Token
        config.setIsShare(securityProperties.isShare());
        // 同一账号最大登录数
        config.setMaxLoginCount(securityProperties.getMaxLoginCount());
        // Token 生成风格（uuid / tik / random-* 等）
        config.setTokenStyle(securityProperties.getTokenStyle());
        // Sa-Token 框架自身日志开关
        config.setIsLog(securityProperties.isLog());
        return config;
    }

    /**
     * 注册 ISecurityService 的 Sa-Token 实现
     * <p>
     * 仅在安全模块启用时生效。当项目中存在其他 {@link ISecurityService} Bean 时不会覆盖。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ISecurityService.class)
    public ISecurityService securityService() {
        return new SaTokenSecurityServiceImpl(securityProperties);
    }

    /**
     * 注册安全拦截器，拦截所有请求
     * <p>
     * 仅在安全模块启用时生效。排除路径从配置动态读取。
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityInterceptor(securityService()))
                .addPathPatterns("/**")
                .excludePathPatterns(securityProperties.getExcludePathPatterns());
    }

    /**
     * 注册 Sa-Token 异常全局处理器
     * <p>
     * 将 Sa-Token 的 {@code NotLoginException}、{@code NotPermissionException} 等
     * 框架异常统一转换为 {@code Result} 响应。通过显式声明 Bean 确保在未启用组件扫描时依然生效。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SaTokenExceptionHandler.class)
    public SaTokenExceptionHandler saTokenExceptionHandler() {
        return new SaTokenExceptionHandler();
    }

    /**
     * 注册默认的 Token 状态监听器（登录/登出/被踢/被顶等事件的日志与清理）
     * <p>
     * 仅在安全模块启用时生效。可通过自定义 {@link SaTokenListener} Bean 覆盖。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SaTokenListener.class)
    public SaTokenListener saTokenListener() {
        return new TokenStateListener();
    }
}
